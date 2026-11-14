---
title: "A plain read before FOR UPDATE let MySQL sell the last seat twice"
date: 2026-11-18
draft: true
description: "On MySQL at its default isolation level, a plain read before SELECT ... FOR UPDATE fixes the transaction's snapshot, so a query that checks a limit after the lock reads stale data. A booking demo overbooked 11% of events at 10 requests per second and 98% at 1,000. Three fixes brought it to zero."
tags: ["mysql", "postgresql", "java", "spring-boot", "jpa", "hibernate", "transactions", "concurrency"]
ShowToc: true
ai_disclosure: ai-generated
references:
  - author: "Oracle"
    title: "Consistent Nonlocking Reads"
    venue: "MySQL 8.4 Reference Manual"
    url: "https://dev.mysql.com/doc/refman/8.4/en/innodb-consistent-read.html"
    checked: "by hand"
  - author: "Oracle"
    title: "Locking Reads"
    venue: "MySQL 8.4 Reference Manual"
    url: "https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html"
    checked: "by hand"
  - author: "PostgreSQL Global Development Group"
    title: "Transaction Isolation"
    venue: "PostgreSQL 17 Documentation"
    url: "https://www.postgresql.org/docs/17/transaction-iso.html"
---

On MySQL at its default isolation level, a query you run after `SELECT ... FOR UPDATE` still reads old data if the transaction read anything before it took the lock. In a small booking service with one plain read before the lock, 11% of events were overbooked at 10 booking requests per second per event, and 98% at 1,000.

The question came from a LinkedIn comment on [the transaction post](/posts/transactional-pool/). A reader asked about splitting a service call into a read phase and a write phase: what if the data you read changes before you write? The usual answer is to lock the row and check again inside the transaction, and on MySQL that isn't always enough.

## The booking

An event has 10 seats. Booking one locks the event row, counts the bookings and inserts a new one if there's room. Before it locks, the method loads the customer:

```java
@Transactional
public boolean book(long eventId, long customerId) {
    Customer customer = customers.findById(customerId).orElseThrow(); // plain read
    Event event = events.lockById(eventId);                           // FOR UPDATE
    if (bookings.countByEventId(eventId) >= event.capacity) {
        return false;
    }
    bookings.save(new Booking(event, customer));
    return true;
}
```

`lockById` is a Spring Data query with `@Lock(PESSIMISTIC_WRITE)`. Hibernate sends these statements to MySQL:

```sql
select c1_0.id,c1_0.name from customer c1_0 where c1_0.id=?
select e1_0.id,e1_0.capacity,e1_0.seats_taken from event e1_0 where e1_0.id=? for update of e1_0
select count(b1_0.id) from booking b1_0 where b1_0.event_id=?
insert into booking (customer_id,event_id) values (?,?)
```

The lock keeps two bookings apart, but it doesn't change what the count reads.

## Why the lock doesn't help

MySQL's default isolation level is REPEATABLE READ. Under it, a transaction's plain `SELECT`s all read one snapshot, a frozen view of the committed data, and that snapshot is taken at the transaction's [first plain read](https://dev.mysql.com/doc/refman/8.4/en/innodb-consistent-read.html), not when the transaction starts. A [locking read](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html) like `FOR UPDATE` is the exception: it always reads the latest committed row. The count isn't a locking read, so it reads from the snapshot.

I forced the interleaving on a 1-seat event with two transactions and a latch:

| Step | Booking A | Booking B |
|---|---|---|
| 1 | Loads the customer, locks the event, counts 0, inserts | |
| 2 | Pauses before its commit | Loads the customer. Its snapshot is fixed here, without A's booking |
| 3 | | Asks for the event lock and waits |
| 4 | Commits | Gets the lock |
| 5 | | Counts 0 in its snapshot and inserts the second booking |

While B waited, MySQL's `performance_schema.data_lock_waits` showed it queued for the exclusive record lock on the event's primary key, so the lock worked, and the count still missed A's booking.

## How often it happens

A booking took about 10 ms. I sent each event 15 booking requests, 5 more than it has seats, arriving at random at a fixed average rate, and ran 100 events per rate in each of 3 rounds.

{{< chart "lines" "arrivals" >}}
Each point is 300 events. The fixes, and PostgreSQL at its default, didn't overbook a single event at any rate.
{{< /chart >}}

Even at 10 requests per second, one every 100 ms on average, 11% of events were overbooked. The faster requests arrive, the more likely one lands while another booking is still running. At 200 and 500 the rounds varied a lot, so I wouldn't read much into the flat part of the curve.

In a flash sale, where every booker keeps trying until the event says it's full, the bookers waiting on the lock when the last seat goes all have a snapshot from before it, and all of them book.

{{< chart "lines" "flash" >}}
With a plain read before the lock, a 10-seat event sold 10 seats plus one for every booker but the first. At 50 bookers, 592 of 600 events sold 59 seats and the other 8 sold between 38 and 56.
{{< /chart >}}

## Three fixes

All three held at zero overbooked events in every run:

1. **Lock first.** Make the `FOR UPDATE` the transaction's first read. The snapshot is then taken by the first plain read after the lock is granted (here, loading the customer), so it includes the previous booking. This is the smallest change, though PostgreSQL at REPEATABLE READ still overbooks with it (see below).
2. **READ COMMITTED for this transaction.** `@Transactional(isolation = Isolation.READ_COMMITTED)` gives every statement a fresh snapshot, so the count after the lock sees the latest commit, and the rest of the application keeps REPEATABLE READ. If your application already runs MySQL at READ COMMITTED, this bug doesn't apply.
3. **A counter on the locked row.** Keep `seats_taken` on the event and update it under the lock. The count then comes from the row the `FOR UPDATE` returned, which is always the latest version. It doesn't matter which plain reads come before the lock, and it's what I'd pick when the schema can change.

   One catch with JPA: if the event entity was already loaded earlier in the transaction, Hibernate keeps that copy and ignores the newer values the `FOR UPDATE` returns. The counter is then stale, and the counter fix overbooked again, on MySQL and on PostgreSQL at its default. `entityManager.refresh(event, LockModeType.PESSIMISTIC_WRITE)` reloads the entity under the lock and fixed it.

Fixes 1 and 2 only hold when the method starts the transaction. Called from a `@Transactional` caller that already read something, both overbooked, because they join the caller's transaction: its snapshot is already fixed, and Spring ignores an isolation level on a method that joins. I didn't test a single conditional `UPDATE ... WHERE seats_taken < capacity` or optimistic locking with `@Version`.

## A second trap: the check inside the locking statement

Folding the count into the locking statement doesn't close the gap either:

```sql
select * from event e
 where e.id = :id
   and (select count(*) from booking b where b.event_id = e.id) < e.capacity
   for update
```

The lock covers the event row, but the subquery is a plain read from the snapshot. [MySQL's manual](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html) says an outer locking clause doesn't lock the rows a subquery reads unless the subquery has its own. After a plain read of the customer, this version overbooked at similar rates to the original, though the two differ at 500 requests per second (see the table). As the transaction's first read it was safe in my test, but I wouldn't build on that, because it depends on MySQL taking the lock before it runs the subquery.

{{< results-table "rates" >}}

## PostgreSQL

PostgreSQL defaults to READ COMMITTED, where each statement takes its own snapshot. I ran the same two-transaction test there:

{{< results-table "checks" >}}

The original code and lock first were both safe at the default.

- The subquery version overbooked on PostgreSQL too. A statement's snapshot is taken when it starts, before it waits for the lock. When the lock is granted, PostgreSQL may recheck the locked row itself, but the count in the subquery keeps the statement's snapshot, so it still misses A's booking.
- At REPEATABLE READ, PostgreSQL takes the snapshot at the transaction's [first statement](https://www.postgresql.org/docs/17/transaction-iso.html), locking or not. Lock first overbooked there like the original. The counter fix did change the event row, so the second booking failed with `could not serialize access due to concurrent update` instead, which the caller has to retry in a new transaction.

## What I'd do

On MySQL, keep the value you check on the row you lock. If that's not possible, make the lock the first read of the whole transaction, including any caller's. Don't put the check in a subquery of the locking statement. If an entity was loaded before you lock its row, lock it with `refresh`, not a query.

## Caveats

One demo app on one machine (Docker on an arm64 Mac, 2 CPUs and 1 GB per database), MySQL 8.4.13 and PostgreSQL 17.6 with their default settings, and a small schema with one index on `booking.event_id`. Each two-transaction check is a single run; I ran the full set four times while building the demo, and every run gave the same outcome. The rates depend on how long a booking takes against how often requests arrive, so the shape of the curve carries over to other systems and the exact numbers don't.

## Method and data

- The app is Spring Boot 4.1.2 with Spring Data JPA 4.1.2, Hibernate 7.4.13 and HikariCP 7.0.2 (pool of 60), on JDK 25, against MySQL 8.4.13 (REPEATABLE READ) and PostgreSQL 17.6 (READ COMMITTED) in containers.
- Two-transaction checks: A books the only seat and pauses before its commit; B runs up to its lock while A is open; the check records which lock B waits on (`performance_schema.data_lock_waits` on MySQL, `pg_locks` on PostgreSQL), then lets A commit. `checks.json`.
- Arrivals: 100 events of 10 seats per rate and round, 15 requests per event with exponential gaps (a Poisson process), one attempt each, the same seeded pattern for every setup. Flash sale: 200 events per bookers count and round, each booker retrying until the event is full. Setups and rates ran in random order in 3 rounds. The PostgreSQL step at 10 requests per second in round 1 was rerun, because the host went to sleep during it. Intervals are Wilson 95% intervals. `results.jsonl`.
- Everything is in [`benchmarks/mysql-lock`](https://github.com/diegourban/diegourban.github.io/tree/master/benchmarks/mysql-lock): `checks.py` runs the checks, `bench.py` runs the rounds, and `make_charts.py` computes every number.
