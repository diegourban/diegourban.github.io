package demo.booking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Runs the booking writers against the configured database and prints one JSON line per result.
 *
 * <pre>
 *   --mode=check  [--variants=a,b]           two transactions on a 1-seat event, interleaved with latches
 *   --mode=hammer --variant=a --concurrency=2,5 [--events=200] [--capacity=10]
 * </pre>
 */
@SpringBootApplication
public class BookingApp implements ApplicationRunner {
    private static final int CUSTOMERS = 60;

    private final JdbcTemplate jdbc;
    private final CustomerRepository customers;
    private final Map<String, BiFunction<Long, Long, Boolean>> variants = new LinkedHashMap<>();
    private final boolean mysql;

    BookingApp(JdbcTemplate jdbc, CustomerRepository customers, BookingWriter writer, BookingFacade facade) {
        this.jdbc = jdbc;
        this.customers = customers;
        variants.put("noLock", writer::noLock);
        variants.put("plainReadFirst", writer::plainReadFirst);
        variants.put("lockFirst", writer::lockFirst);
        variants.put("readCommitted", writer::readCommitted);
        variants.put("counter", writer::counter);
        variants.put("subquery", writer::subquery);
        variants.put("subqueryFirst", writer::subqueryFirst);
        variants.put("existsFirst", writer::existsFirst);
        variants.put("referenceFirst", writer::referenceFirst);
        variants.put("counterEventLoadedFirst", writer::counterEventLoadedFirst);
        variants.put("counterEventLoadedThenRefreshed", writer::counterEventLoadedThenRefreshed);
        variants.put("outerRead", facade::outerRead);
        variants.put("outerReadNewTransaction", facade::outerReadNewTransaction);
        this.mysql = jdbc.execute((java.sql.Connection c) -> c.getMetaData().getDatabaseProductName()).toLowerCase().contains("mysql");
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(BookingApp.class, args)));
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (customers.count() == 0) {
            customers.saveAll(IntStream.rangeClosed(1, CUSTOMERS).mapToObj(i -> new Customer((long) i, "customer " + i)).toList());
        }
        String mode = arg(args, "mode", "check");
        if (mode.equals("check")) {
            List<String> names = Arrays.asList(arg(args, "variants", String.join(",", variants.keySet())).split(","));
            // Warm-up, discarded: the first run in a cold JVM can take seconds to reach its lock.
            for (String name : names) {
                check(name);
            }
            for (String name : names) {
                System.out.println(check(name));
            }
        } else if (mode.equals("arrivals")) {
            String name = arg(args, "variant", "plainReadFirst");
            int events = Integer.parseInt(arg(args, "events", "100"));
            int capacity = Integer.parseInt(arg(args, "capacity", "10"));
            int requests = Integer.parseInt(arg(args, "requests", "15"));
            long seed = Long.parseLong(arg(args, "seed", "1"));
            for (String r : arg(args, "rates", "50,100,200,500,1000").split(",")) {
                System.out.println(arrivals(name, Integer.parseInt(r), events, capacity, requests, seed));
            }
        } else {
            String name = arg(args, "variant", "plainReadFirst");
            int events = Integer.parseInt(arg(args, "events", "200"));
            int capacity = Integer.parseInt(arg(args, "capacity", "10"));
            for (String c : arg(args, "concurrency", "2,5,10,20,50").split(",")) {
                System.out.println(hammer(name, Integer.parseInt(c), events, capacity));
            }
        }
    }

    private static String arg(ApplicationArguments args, String name, String fallback) {
        List<String> v = args.getOptionValues(name);
        return v == null || v.isEmpty() ? fallback : v.get(0);
    }

    private void reset(int events, int capacity) {
        jdbc.update("delete from booking");
        jdbc.update("delete from event");
        jdbc.batchUpdate("insert into event (id, capacity, seats_taken) values (?, ?, 0)",
                IntStream.rangeClosed(1, events).mapToObj(i -> new Object[] {i, capacity}).toList());
    }

    private String isolation() {
        return jdbc.queryForObject(mysql ? "select @@transaction_isolation" : "show transaction_isolation", String.class);
    }

    /** The locks other sessions are waiting for, as "table/index mode", empty when nobody waits. */
    private List<String> lockWaits() {
        return jdbc.queryForList(mysql
                ? """
                  select concat(l.object_name, '/', coalesce(l.index_name, '-'), ' ', l.lock_mode)
                    from performance_schema.data_lock_waits w
                    join performance_schema.data_locks l on l.engine_lock_id = w.requesting_engine_lock_id"""
                : """
                  select coalesce(relation::regclass::text, locktype) || ' ' || mode
                    from pg_locks where not granted""", String.class);
    }

    /**
     * T1 books the only seat and stops just before its commit. T2 starts while T1 is open, runs up to its lock, and
     * blocks on T1's lock (or doesn't, for the writers that take none). Then T1 commits and T2 finishes.
     */
    private String check(String name) throws Exception {
        BiFunction<Long, Long, Boolean> variant = variants.get(name);
        if (variant == null) {
            throw new IllegalArgumentException("unknown variant " + name);
        }
        reset(1, 1);
        CountDownLatch t1AtCommit = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch t2BeforeLock = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> t1 = pool.submit(() -> {
                Hooks.set(point -> {
                    if (point.equals("beforeCommit")) {
                        t1AtCommit.countDown();
                        await(release);
                    }
                });
                try {
                    return variant.apply(1L, 1L);
                } finally {
                    Hooks.clear();
                }
            });
            boolean t1Paused = t1AtCommit.await(10, TimeUnit.SECONDS);
            Future<Boolean> t2 = pool.submit(() -> {
                Hooks.set(point -> {
                    if (point.equals("beforeLock")) {
                        t2BeforeLock.countDown();
                    }
                });
                try {
                    return variant.apply(1L, 2L);
                } finally {
                    Hooks.clear();
                }
            });
            t2BeforeLock.await(10, TimeUnit.SECONDS);
            boolean blocked = false;
            List<String> waitingFor = List.of();
            for (int i = 0; i < 200 && !t2.isDone(); i++) {
                waitingFor = lockWaits();
                if (!waitingFor.isEmpty()) {
                    blocked = true;
                    break;
                }
                Thread.sleep(50);
            }
            boolean t2DoneBeforeCommit = t2.isDone();
            release.countDown();
            String r1 = outcome(t1);
            String r2 = outcome(t2);
            int booked = jdbc.queryForObject("select count(*) from booking", Integer.class);
            int seatsTaken = jdbc.queryForObject("select seats_taken from event where id = 1", Integer.class);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("mode", "check");
            out.put("db", mysql ? "mysql" : "postgresql");
            out.put("isolation", isolation());
            out.put("variant", name);
            out.put("capacity", 1);
            out.put("bookings", booked);
            out.put("seatsTaken", seatsTaken);
            out.put("overbooked", booked > 1);
            out.put("t1", r1 + (t1Paused ? "" : " (never reached its commit)"));
            out.put("t2", r2);
            out.put("t2Blocked", blocked);
            out.put("t2WaitingFor", String.join("; ", waitingFor));
            out.put("t2DoneBeforeT1Commit", t2DoneBeforeCommit);
            return json(out);
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String outcome(Future<Boolean> f) throws InterruptedException {
        try {
            return Boolean.TRUE.equals(f.get(60, TimeUnit.SECONDS)) ? "booked" : "full";
        } catch (ExecutionException e) {
            Throwable root = e.getCause();
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            return "error: " + e.getCause().getClass().getSimpleName() + ": " + root.getMessage();
        } catch (java.util.concurrent.TimeoutException e) {
            return "error: timed out";
        }
    }

    /**
     * c threads book seats for one event at a time until it says it is full; each thread is a different customer. Then
     * count the bookings per event.
     */
    private String hammer(String name, int c, int events, int capacity) throws Exception {
        BiFunction<Long, Long, Boolean> variant = variants.get(name);
        reset(events, capacity);
        ExecutorService pool = Executors.newFixedThreadPool(c);
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        Map<String, AtomicInteger> errorTypes = new java.util.concurrent.ConcurrentHashMap<>();
        long t0 = System.nanoTime();
        try {
            for (long e = 1; e <= events; e++) {
                long eventId = e;
                CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> all = new ArrayList<>();
                for (int t = 0; t < c; t++) {
                    long customerId = t + 1;
                    all.add(pool.submit(() -> {
                        await(start);
                        int failures = 0;
                        while (failures < 50) {
                            attempts.incrementAndGet();
                            try {
                                if (!variant.apply(eventId, customerId)) {
                                    return;
                                }
                            } catch (RuntimeException ex) {
                                failures++;
                                errors.incrementAndGet();
                                errorTypes.computeIfAbsent(ex.getClass().getSimpleName(), k -> new AtomicInteger()).incrementAndGet();
                            }
                        }
                    }));
                }
                start.countDown();
                for (Future<?> f : all) {
                    f.get(120, TimeUnit.SECONDS);
                }
            }
        } finally {
            pool.shutdownNow();
        }
        double seconds = (System.nanoTime() - t0) / 1e9;
        Map<Integer, Integer> sold = new TreeMap<>();
        jdbc.query("select e.id, count(b.id) from event e left join booking b on b.event_id = e.id group by e.id",
                rs -> {
                    sold.merge(rs.getInt(2), 1, Integer::sum);
                });
        int overbooked = sold.entrySet().stream().filter(x -> x.getKey() > capacity).mapToInt(Map.Entry::getValue).sum();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "hammer");
        out.put("db", mysql ? "mysql" : "postgresql");
        out.put("isolation", isolation());
        out.put("variant", name);
        out.put("concurrency", c);
        out.put("events", events);
        out.put("capacity", capacity);
        out.put("overbooked", overbooked);
        out.put("sold", sold);
        out.put("attempts", attempts.get());
        out.put("errors", errors.get());
        out.put("errorTypes", errorTypes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue().get())));
        out.put("seconds", Math.round(seconds * 1000) / 1000.0);
        return json(out);
    }

    /**
     * Open arrivals: each event gets {@code requests} booking requests (more than its capacity) at random times, a
     * Poisson process at {@code rate} requests per second, each from a different customer and each a single attempt. The
     * events run one after another. The arrival times depend only on the seed, the rate and the event, so every writer
     * sees the same pattern.
     */
    private String arrivals(String name, int rate, int events, int capacity, int requests, long seed) throws Exception {
        BiFunction<Long, Long, Boolean> variant = variants.get(name);
        reset(events, capacity);
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        AtomicInteger errors = new AtomicInteger();
        Map<String, AtomicInteger> errorTypes = new java.util.concurrent.ConcurrentHashMap<>();
        java.util.concurrent.atomic.LongAdder busyNanos = new java.util.concurrent.atomic.LongAdder();
        long t0 = System.nanoTime();
        try {
            for (long e = 1; e <= events; e++) {
                long eventId = e;
                java.util.Random random = new java.util.Random(seed * 1_000_003L + rate * 10_007L + e);
                List<Future<?>> all = new ArrayList<>();
                long start = System.nanoTime();
                double at = 0;
                for (int i = 0; i < requests; i++) {
                    at += -Math.log(1 - random.nextDouble()) / rate;
                    long due = start + (long) (at * 1e9);
                    while (System.nanoTime() < due) {
                        java.util.concurrent.locks.LockSupport.parkNanos(Math.min(200_000, due - System.nanoTime()));
                    }
                    long customerId = i + 1;
                    all.add(pool.submit(() -> {
                        long s = System.nanoTime();
                        try {
                            variant.apply(eventId, customerId);
                        } catch (RuntimeException ex) {
                            errors.incrementAndGet();
                            errorTypes.computeIfAbsent(ex.getClass().getSimpleName(), k -> new AtomicInteger()).incrementAndGet();
                        } finally {
                            busyNanos.add(System.nanoTime() - s);
                        }
                    }));
                }
                for (Future<?> f : all) {
                    f.get(120, TimeUnit.SECONDS);
                }
            }
        } finally {
            pool.shutdownNow();
        }
        double seconds = (System.nanoTime() - t0) / 1e9;
        Map<Integer, Integer> sold = new TreeMap<>();
        jdbc.query("select e.id, count(b.id) from event e left join booking b on b.event_id = e.id group by e.id",
                rs -> {
                    sold.merge(rs.getInt(2), 1, Integer::sum);
                });
        int overbooked = sold.entrySet().stream().filter(x -> x.getKey() > capacity).mapToInt(Map.Entry::getValue).sum();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "arrivals");
        out.put("db", mysql ? "mysql" : "postgresql");
        out.put("isolation", isolation());
        out.put("variant", name);
        out.put("rate", rate);
        out.put("requests", requests);
        out.put("seed", seed);
        out.put("events", events);
        out.put("capacity", capacity);
        out.put("overbooked", overbooked);
        out.put("sold", sold);
        out.put("errors", errors.get());
        out.put("errorTypes", errorTypes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue().get())));
        out.put("meanRequestMs", Math.round(busyNanos.sum() / 1e6 / (events * (double) requests) * 1000) / 1000.0);
        out.put("seconds", Math.round(seconds * 1000) / 1000.0);
        return json(out);
    }

    /** Minimal JSON for flat maps of strings, numbers, booleans and nested maps. */
    private static String json(Object o) {
        if (o instanceof Map<?, ?> m) {
            return m.entrySet().stream().map(e -> json(String.valueOf(e.getKey())) + ": " + json(e.getValue()))
                    .collect(Collectors.joining(", ", "{", "}"));
        }
        if (o instanceof Number || o instanceof Boolean) {
            return String.valueOf(o);
        }
        return "\"" + String.valueOf(o).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\"";
    }
}
