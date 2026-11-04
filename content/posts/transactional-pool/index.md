---
title: "@Transactional holds a pool connection it may never use. Spring Boot 4.1 can fix that"
date: 2026-11-10
draft: true
description: "A @Transactional method takes a pool connection when it starts, even on a path that only calls another service. With a pool of 10 and a 300 ms partner API, an order service started failing at 40 req/s when 90% or more of its orders went to the partner. With Spring Boot 4.1's lazy connection fetching, nothing failed up to 300 req/s, the highest rate tested."
tags: ["java", "spring-boot", "spring", "jpa", "hikaricp", "transactions", "performance"]
ShowToc: true
cover:
  image: cover.png
  relative: true
  alt: "Failed requests as the arrival rate rises: with eager connection fetching, an order service starts failing at 40 req/s when every order goes to a partner API, at 70 with half and at 150 with a quarter, while lazy fetching and a narrowed transaction never fail up to 300 req/s"
  hiddenInSingle: true
  hiddenInList: true
ai_disclosure: ai-generated
references:
  - author: "Spring Boot"
    title: "Support LazyConnectionDataSourceProxy (issue #15480)"
    venue: "spring-projects/spring-boot, GitHub"
    year: 2018
    url: "https://github.com/spring-projects/spring-boot/issues/15480"
  - author: "Spring Boot"
    title: "Lazy connection fetching prevents Hikari from registering its MBeans (pull request #52373)"
    venue: "spring-projects/spring-boot, GitHub"
    url: "https://github.com/spring-projects/spring-boot/pull/52373"
  - author: "Spring Boot"
    title: "SQL Databases"
    venue: "Spring Boot reference documentation"
    url: "https://docs.spring.io/spring-boot/reference/data/sql.html"
  - author: "Spring Framework"
    title: "LazyConnectionDataSourceProxy"
    venue: "Spring Framework Javadoc"
    url: "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/datasource/LazyConnectionDataSourceProxy.html"
  - author: "Spring Framework"
    title: "Propagation"
    venue: "Spring Framework Javadoc"
    url: "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Propagation.html"
  - author: "Spring Framework"
    title: "Transaction Propagation"
    venue: "Spring Framework reference documentation"
    url: "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html"
  - author: "Hodges, J. L. and Lehmann, E. L."
    title: "Estimates of Location Based on Rank Tests"
    venue: "The Annals of Mathematical Statistics 34(2), 598–611"
    year: 1963
    url: "https://doi.org/10.1214/aoms/1177704172"
    checked: "by hand"
  - author: "Efron, B."
    title: "Bootstrap Methods: Another Look at the Jackknife"
    venue: "The Annals of Statistics 7(1), 1–26"
    year: 1979
    url: "https://doi.org/10.1214/aos/1176344552"
    checked: "by hand"
  - author: "Mihalcea, V."
    title: "How to use LazyConnectionDataSourceProxy with Spring Data JPA"
    venue: "vladmihalcea.com"
    url: "https://vladmihalcea.com/lazyconnectiondatasourceproxy-spring-data-jpa/"
---

A `@Transactional` method takes a connection from the pool the moment it starts, whether or not it ever runs a query. I tested it with an order service that writes the order to the database for products it stocks and forwards marketplace orders to a partner API, which took 300 ms to answer. With a pool of 10 connections, it started failing at 40 requests per second when 90% or more of the orders went to the partner. Those orders never needed the database. They held a connection while they waited for the partner, and the orders that did need one queued behind them until the pool timed out. Spring Boot 4.1 added a property that fetches the connection only when the first statement runs. With it, nothing failed up to 300 requests per second, the highest rate I tested.

## The method

```java
@Transactional
public String placeOrder(String productId, int quantity) {
    if (isOwnStock(productId)) {
        PurchaseOrder order = orders.save(new PurchaseOrder(productId, quantity));
        stock.decrement(productId, quantity);
        return "placed:" + order.getId();
    }
    return "forwarded:" + forward(productId, quantity);
}
```

An order for a product the shop stocks is two writes, the order and the stock, which is why the method is transactional. An order for a marketplace product goes to the partner's API and never touches the database. The catalog rule behind `isOwnStock` lives in memory, as a cache would; if it were a query, every order would run one before the partner call, which is the third case below.

Calling a third-party API from inside a transaction like this isn't how I'd design it. Sending the order asynchronously, through an outbox or a queue, is usually the better design, and making the call outside the transaction is the next best (that's the narrowed setup below). A real client would also set a timeout, which the example doesn't. The example is here to show what the pool does with the code as many services have it.

## What I measured

The app is a small Spring Boot 4.1.2 service with Spring Data JPA and PostgreSQL 17.6, and a WireMock stub plays the partner API with a fixed 300 ms delay. HikariCP had 10 connections and gave up after 2 seconds of waiting for one. Each part ran in its own Docker container with 2 CPUs.

k6 sent the load at fixed arrival rates, from 10 to 300 requests per second in 30-second steps. A tool that waits for each answer before sending the next request would quietly slow down along with the app, and the pool would never run out. The share of orders sent to the partner was 100%, 95%, 90%, 75%, 50% or 25%, in a fixed pattern over every 20 orders, and the own-stock orders spread over 100 stock rows so they didn't queue on one row lock. I ran three setups:

- **Eager**: the method above. Open session in view is off; everything else is at Spring Boot's defaults.
- **Lazy**: the same code with `spring.datasource.connection-fetch=lazy`.
- **Narrowed**: `placeOrder` itself isn't transactional, and the own-stock branch calls a separate `@Transactional` bean that does the two writes. This is the refactor you'd do by hand.

Each combination ran three times, in random order. A step failed when more than 1% of its requests failed, and a run stopped after three failing steps in a row.

{{< chart "lines" "breaks" >}}
Each eager run first failed at the step just above the capacity calculated from the hold times, and every round failed at the same step.
{{< /chart >}}

{{< results-table >}}

With eager fetching the pool ran out at 40 req/s whenever 90% or more of the orders went to the partner, at 50 with 75%, at 70 with half and at 150 with a quarter. Lazy and narrowed never failed, at any rate or share. With every order going to the partner, which is the method that never needs the database, the pool samples (every half second) never showed an active connection.

{{< chart "lines" "failures" >}}
The failures are the pool's 2-second timeout, plus, at a quarter partner orders from 200 req/s, requests k6 couldn't send in time. Both kinds of order failed, at similar rates.
{{< /chart >}}

Lazy and narrowed were close in latency too. Narrowed's own-stock orders were slower in 12 of 15 paired runs, by about 0.3 ms at p95 (95% CI −0.04 to +0.8 ms), which I'd ignore. Eager and lazy only compare fairly while no thread waits for a connection, which here means the first few, low-rate steps of each run, and there they were within a few percent of each other (95% CI −2.9% to +4.1%).

## The arithmetic behind it

A partner order holds its connection for the whole call, about 309 ms measured, so 10 connections cover about 32 of them per second. Add the 3.5 ms an own-stock order holds one for its two writes, and the pool's capacity is 10 ÷ (share × 0.309 s + (1 − share) × 0.0035 s). That gives about 32 orders per second when they all go to the partner, 36 at 90%, 43 at 75%, 64 at half and 125 at a quarter. Every eager run failed at the first step above that: 40, 50, 70 and 150. The hold times come from the same runs, so this shows the arithmetic is consistent rather than predicting anything, and at a quarter partner orders the capacity of about 125 sits right on the 125 req/s step that still passed.

So the breakpoint depends mostly on the pool size, the partner's latency and the share of orders that go to it. The database work only matters when that share is small. By the same arithmetic, a partner that slows to 600 ms during an incident halves the breakpoint. I didn't test that.

And the own-stock orders, the ones that need the database, went from fine to failing within a few steps.

{{< chart "lines" "latency" >}}
The early fall in every line is the JVM warming up, because the rates always ran from low to high on a freshly started app.
{{< /chart >}}

With half the orders going to the partner, the own-stock orders' p95 was 11 ms at 30 req/s, 64 ms at 40 and 145 ms at 60. At 70 it was about 2 seconds, which is the pool timeout. From there on an own-stock order either got a connection or failed after two seconds, with about 140 threads queued for one (137 to 141 across the rounds).

The pool's waiting-threads gauge, `hikaricp.connections.pending`, warned earlier the more own-stock orders there were. At half and at a quarter partner orders, threads waited three steps before the failures, and at 75% one step before. At 90% and above, nothing waited at 30 req/s and 40 already failed.

## The flag

`spring.datasource.connection-fetch=lazy` arrived in Spring Boot 4.1 ([issue #15480](https://github.com/spring-projects/spring-boot/issues/15480)). When a transaction begins, Spring's transaction managers switch off auto-commit on a connection (with JPA, Hibernate does it as its transaction begins), so the connection is taken as the method is entered. The flag wraps the auto-configured pool in Spring's [`LazyConnectionDataSourceProxy`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/datasource/LazyConnectionDataSourceProxy.html), which records those settings and applies them to a real connection only when the first statement needs one. Before 4.1 you wired the proxy yourself, as [Vlad Mihalcea describes](https://vladmihalcea.com/lazyconnectiondatasourceproxy-spring-data-jpa/).

The [reference documentation](https://docs.spring.io/spring-boot/reference/data/sql.html) talks about JDBC transactions only, so I checked JPA too, by reading the pool's active connections during the downstream call on an idle app. A plain `@Transactional` method and a `readOnly = true` one each held one connection with eager fetching and none with lazy fetching, under `JpaTransactionManager` and the JDBC transaction manager alike.

The property only applies to the pool Spring Boot creates. An app that replaces it with its own `DataSource` bean gets no proxy and no warning, and still needs the manual wiring. Spring Boot's own recipe for a second database keeps the auto-configured pool, so the flag covers that one, but not the second. Where it does apply, the `dataSource` bean becomes the proxy, so code that injects or casts to `HikariDataSource` breaks, while `DataSource.unwrap(HikariDataSource.class)` still reaches the pool.

With lazy fetching, a pool timeout surfaces later in the method, after anything it did before its first query. I gave the pool a single connection, held it, and called a method that makes the slow call and then saves. Eager fetching failed before the call, with `CannotCreateTransactionException`. Under lazy fetching the method made the call and then failed at the save, with `DataAccessResourceFailureException`. Retry and error handling written for the first exception won't see the second, and a call that changes something on the other side has already happened.

From 4.1.0 to 4.1.2, lazy fetching with JMX and Hikari's `register-mbeans` both on also stopped Hikari registering its pool MBeans ([#52373](https://github.com/spring-projects/spring-boot/pull/52373)). The fix is planned for 4.1.3, which isn't out as I write this. My runs used 4.1.2 with JMX off, its default, and read the pool through Micrometer.

## What each kind of method holds

These are all the single-request checks, on simpler methods than `placeOrder`. Each number is the pool's active connections during a 300 ms downstream call, on an otherwise idle app, three repeats each. Every repeat agreed. OSIV is open session in view.

{{< results-table "checks" >}}

### A method that never touches the database

Don't make it transactional. If the class is annotated with `@Transactional`, opt the method out with `@Transactional(propagation = Propagation.NOT_SUPPORTED)`, or with `NEVER`, which also [throws](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Propagation.html) if someone later calls it from inside a transaction. Neither held a connection. `readOnly = true` doesn't help, since with eager fetching it held one, exactly like a plain `@Transactional`.

### Some paths touch the database and some don't

That's `placeOrder`, and the case the flag is for. Keep the transaction, turn on lazy fetching, and the path that never runs a query never takes a connection. A method that made the call first and queried after also held nothing during the call, so the order within a path matters too.

### A query first, then the call

Here the flag can't help, because once the first query has run, the connection stays with the transaction until it ends. Move the call before the query, or narrow the transaction to the part that needs it.

### Where opting out doesn't release the connection

A `NOT_SUPPORTED` or `SUPPORTS` method that runs its own query keeps the connection. One that ran an `EntityManager` query and then made the call held a connection through the call, under eager and lazy fetching alike. Neither annotation starts a transaction, but both still turn on Spring's transaction synchronization for the method, and Spring binds one `EntityManager` to it, which keeps its connection until the method returns. Lazy fetching can't help, because the query has already fetched the real connection. According to the [`SUPPORTS` documentation](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Propagation.html), the same resources are shared for the entire scope. With no annotation at all, the same method held nothing. Spring Data repository calls behave differently again, because each repository method runs in its own short transaction (read-only for the query methods) and gives the connection back when it commits. `repo.count()` followed by the call held nothing under `SUPPORTS`, `NOT_SUPPORTED` or no annotation.

A suspended transaction keeps its connection. An outer `@Transactional` method that had run a query and then called a `NOT_SUPPORTED` method still held its connection during that call, with lazy fetching too. With `REQUIRES_NEW` instead, the inner transaction took a second connection while the outer one kept its first, so one request held two. The [Spring documentation](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html) warns that this can exhaust the pool.

Open session in view keeps one `EntityManager` open for the whole request, and Hibernate holds its connection from the first query until the response is written. With it on, every method that queried and then made the call held a connection, transaction or not. It's [on by default](https://docs.spring.io/spring-boot/reference/data/sql.html) for Spring Boot web apps, and Spring Boot logs a warning about it at startup.

## What I'd do

If a transactional method makes a slow call on some paths, and you can't move the call out, and Spring Boot creates your pool, turn on `spring.datasource.connection-fetch=lazy`. If you also export Hikari's MBeans over JMX, wait for 4.1.3. Check your error handling for the later pool timeout. Then turn off open session in view, and look at the methods that query before a slow call. They need the call moved or the transaction narrowed, and the flag doesn't change that.

## Caveats

One small app on one machine (Docker Desktop on an arm64 Mac), PostgreSQL only, one fixed partner latency, a pool of 10, and three rounds. The rates scale with the pool size and the latency, so read the breakpoints as a ratio, not as numbers to expect in production. A real partner API also has jitter, which I left out to keep the arithmetic clean.

k6 sent orders at perfectly even intervals with a fixed pattern. Real traffic is burstier, so waits and failures start somewhat below these rates, and the cliff is less sharp. With a 30-second step and a 2-second timeout, a step also has to be a little above capacity before 1% of its requests time out.

The load test ran with open session in view off. With it on, the checks show the partner path holding the same connections (one with eager fetching, none with lazy), but an own-stock order would keep its connection a little longer, until the response is written. I'd expect similar breakpoints, and I didn't measure them. All runs used platform threads. I didn't measure Tomcat's threads directly, but the pool counters put about 200 requests in flight, all of Tomcat's 200 threads, only at a quarter partner orders from 150 req/s. Virtual threads wouldn't move the breakpoints, which the pool sets, but past them more requests would reach the pool and time out, where platform threads leave them waiting for a Tomcat thread.

The partner client was the JDK's `HttpClient`, which doesn't limit connections per host. Apache HttpClient 5, which Spring Boot prefers when it's on the classpath, allows 5 per host by default, about 16 calls per second at 300 ms. With eager fetching, an order waiting for one of those would hold its database connection while it waits. I didn't test that.

## Method and data

- The app (`placeOrder` in `orders/`, the single-request checks in `Checks.java`), the WireMock stubs, the k6 script and the drivers are in [`benchmarks/tx-pool`](https://github.com/diegourban/diegourban.github.io/tree/master/benchmarks/tx-pool). The app sets `spring.jpa.open-in-view=false`, the pool size and the timeout, and declares its own `JpaTransactionManager` next to a JDBC one for the checks. The open session in view default (`true`) and its startup warning are in the configuration metadata and classes of `spring-boot-jpa` 4.1.2.
- `checks.py` runs the single-request checks, three repeats each, and the pool-of-1 contention check (`checks.json`). `bench.py` runs the load steps (`results.jsonl`).
- Latency is k6's p95 per route and step. Every chart and table value is the median of the three rounds. The breakpoints were the same step in every round, while the failure rate at a failing step varied (3.0% to 3.4% at half partner orders and 70 req/s). A request counts as failed when it returns an error or k6 couldn't send it on time. k6 dropped requests only in two overloaded steps, eager at a quarter partner orders at 200 and 250 req/s, past its breakpoint.
- The pool figures are instantaneous samples from Actuator every half second, so short waits between samples aren't counted.
- The comparisons between setups pair runs by share and round (15 pairs for the own-stock orders, which don't exist at 100% partner orders). Their shift is the [Hodges-Lehmann estimate](https://doi.org/10.1214/aoms/1177704172) of the per-run median difference, its 95% interval a [bootstrap](https://doi.org/10.1214/aos/1176344552) (10,000 resamples, fixed seed), with the same code as the earlier posts. `analyze.py` computes those and the capacity model (`analysis.json`), and `make_charts.py` every number in the charts and tables.
