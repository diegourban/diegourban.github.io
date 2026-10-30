---
title: "@Transactional holds a pool connection it may never use. Spring Boot 4.1 can fix that"
date: 2026-11-04
draft: true
description: "A @Transactional method takes a pool connection when it starts, even on a path that only calls another service. With a pool of 10 and a 300 ms downstream call, requests started failing at 40 req/s when 90% of them made that call. With Spring Boot 4.1's lazy connection fetching, nothing failed up to 300 req/s, the highest rate tested."
tags: ["java", "spring-boot", "spring", "jpa", "hikaricp", "transactions", "performance"]
ShowToc: true
cover:
  image: cover.png
  relative: true
  alt: "Failed requests as the arrival rate rises: with eager connection fetching, 90% downstream requests start failing at 40 req/s and 50% at 70 req/s, while lazy fetching and a narrowed transaction never fail up to 300 req/s"
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

A `@Transactional` method takes a connection from the pool the moment it starts, whether or not it ever runs a query. In my test, with a pool of 10 connections and a downstream call that took 300 ms, a method that wrote to the database on one path and made that call on the other started failing at 40 requests per second, when 90% of requests made the call. Those requests never needed the database. They held a connection while they waited, and the requests that did need one queued behind them until the pool timed out. Spring Boot 4.1 added a property that fetches the connection only when the first statement runs. With it, nothing failed up to 300 requests per second, the highest rate I tested.

## The method

Here's the method I tested:

```java
@Transactional
public String handle(String route) {
    if ("db".equals(route)) {
        return "db:" + repo.save(new Item("x")).getId();
    }
    return "downstream:" + downstream.call();
}
```

The database path saves a row. On the other branch the method calls another service, which takes 300 ms to answer, and never runs a query. In real code the database path does more than one write, which is why the method is transactional at all. The example keeps a single `save` to stay short.

## What I measured

The app is a small Spring Boot 4.1.2 service with Spring Data JPA and PostgreSQL 17.6, and a WireMock stub plays the downstream service with a fixed 300 ms delay. HikariCP had 10 connections and gave up after 2 seconds of waiting for one. Each part ran in its own Docker container with 2 CPUs.

k6 sent the load at fixed arrival rates, from 10 to 300 requests per second in 30-second steps. A tool that waits for each answer before sending the next request would quietly slow down along with the app, and the pool would never run out. The share of requests on the downstream path was 10%, 50% or 90%, in a fixed pattern where 1, 5 or 9 out of every 10 requests made the call. I ran three setups:

- **Eager**: the method above. Open session in view is off; everything else is at Spring Boot's defaults.
- **Lazy**: the same code with `spring.datasource.connection-fetch=lazy`.
- **Narrowed**: the method itself isn't transactional, and the database path calls a separate `@Transactional` bean that does the writes. This is the refactor you'd do by hand.

Each combination ran three times, in random order. A step failed when more than 1% of its requests failed, and a run stopped after three failing steps in a row.

{{< chart "lines" "failures" >}}
The failures are the pool's 2-second timeout.
{{< /chart >}}

{{< results-table >}}

All three rounds broke at the same step. With 90% of requests on the downstream path, eager fetching held up at 30 req/s and failed at 40, and with half of them it held at 60 and failed at 70. Lazy and narrowed never failed, at any rate or share.

At the first failing step, the database requests failed more often than the downstream ones. At 90% and 40 req/s, 4.2% to 6.7% of the database requests failed across the three rounds, against 3.7% to 3.9% of the downstream ones.

Lazy and narrowed were also close in latency. Narrowed's database p95 was higher by about 0.3 ms (95% CI 0.1 to 1.1 ms, higher in 7 of 9 paired runs), small enough that I'd ignore it. And as long as no thread waited for a connection, the database p95 of eager and lazy differed by 0.2% (95% CI −2.7% to +2.4%).

## The arithmetic behind it

A downstream request holds its connection for the whole call, about 307 ms measured, so 10 connections cover about 32 downstream requests per second. Add the 3.5 ms a database request holds one and the pool's capacity is 10 ÷ (share × 0.307 s + (1 − share) × 0.0035 s). That gives about 36 requests per second at 90% downstream, and the pool ran out between 30 and 40. Half and half gives about 64, and it ran out between 60 and 70. At 10% the formula says about 295, right where I stopped, and at 300 req/s a few threads waited at a time (never more than 3 in a sample) without a queue building up.

So the breakpoint depends mostly on the pool size, the downstream latency and the share of traffic that makes the call. The database work only matters when that share is small. By the same arithmetic, doubling the downstream latency during an incident halves the breakpoint. I didn't test that.

And the database requests went from fine to failing within two steps, with no gradual slowdown before it.

{{< chart "lines" "latency" >}}
The early fall in every line is the JVM warming up, because the rates always ran from low to high on a freshly started app.
{{< /chart >}}

With 50% of requests on the downstream path, the database requests' p95 was 9 ms at 40 req/s and 64 ms at 60. At 70 it was about 2 seconds, which is the pool timeout. From there on a database request either got a connection or failed after two seconds, with about 140 threads queued for one (136 to 141 across the rounds).

Nor did the pool's waiting-threads gauge, `hikaricp.connections.pending`, give much warning. With half the requests on the downstream path it showed waiting threads one or two steps before the failures, and at 90% nothing waited at 30 req/s while 40 already failed.

## The flag

`spring.datasource.connection-fetch=lazy` arrived in Spring Boot 4.1 ([issue #15480](https://github.com/spring-projects/spring-boot/issues/15480)). When a transaction begins, Spring's transaction managers switch off auto-commit on a connection (with JPA, Hibernate does it as its transaction begins), so the connection is taken as the method is entered. The flag wraps the auto-configured pool in Spring's [`LazyConnectionDataSourceProxy`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/datasource/LazyConnectionDataSourceProxy.html), which records those settings and applies them to a real connection only when the first statement needs one. Before 4.1 you wired the proxy yourself, as [Vlad Mihalcea describes](https://vladmihalcea.com/lazyconnectiondatasourceproxy-spring-data-jpa/). Hibernate also has its own setting to skip the auto-commit switch, `hibernate.connection.provider_disables_autocommit`, which I didn't test.

The [reference documentation](https://docs.spring.io/spring-boot/reference/data/sql.html) talks about JDBC transactions only, so I checked JPA too, by reading the pool's active connections during the downstream call on an idle app. A plain `@Transactional` method and a `readOnly = true` one each held one connection with eager fetching and none with lazy fetching, under `JpaTransactionManager` and the JDBC transaction manager alike.

The property only applies when Spring Boot creates the pool. An app that defines its own `DataSource` bean, as every setup with more than one database does, gets no proxy and no warning, and still needs the manual wiring. Where it does apply, the `dataSource` bean becomes the proxy, so code that injects or casts to `HikariDataSource` breaks, while `DataSource.unwrap(HikariDataSource.class)` still reaches the pool.

With lazy fetching, a pool timeout surfaces later in the method, after anything it did before its first query. I gave the pool a single connection, held it, and called a method that makes the downstream call and then saves. Eager fetching failed before the call, with `CannotCreateTransactionException`. Under lazy fetching the method made the call and then failed at the save, with `DataAccessResourceFailureException`. Retry and error handling written for the first exception won't see the second, and a call that changes something on the other side has already happened.

From 4.1.0 to 4.1.2, lazy fetching with JMX and Hikari's `register-mbeans` both on also stopped Hikari registering its pool MBeans ([#52373](https://github.com/spring-projects/spring-boot/pull/52373)). It's fixed in 4.1.3. My runs used 4.1.2 with JMX off, its default, and read the pool through Micrometer.

## What each kind of method holds

These are all the single-request checks. Each number is the pool's active connections during the 300 ms downstream call, on an otherwise idle app, three repeats each. Every repeat agreed. OSIV is open session in view.

{{< results-table "checks" >}}

### A method that never touches the database

Don't make it transactional. If the class is annotated with `@Transactional`, opt the method out with `@Transactional(propagation = Propagation.NOT_SUPPORTED)`, or with `NEVER`, which also [throws](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Propagation.html) if someone later calls it from inside a transaction. Neither held a connection. `readOnly = true` doesn't help, since with eager fetching it held one, exactly like a plain `@Transactional`.

### Some paths touch the database and some don't

That's the method above, and the case the flag is for. Keep the transaction, turn on lazy fetching, and the path that never runs a query never takes a connection. A method that made the call first and queried after also held nothing during the call, so the order within a path matters too.

### A query first, then the call

Here the flag can't help, because once the first query has run, the connection stays with the transaction until it ends. Move the call before the query, or narrow the transaction to the part that needs it.

### Where opting out doesn't release the connection

A `NOT_SUPPORTED` or `SUPPORTS` method that runs its own query keeps the connection. One that ran an `EntityManager` query and then made the call held a connection through the call, under eager and lazy fetching alike. Neither annotation starts a transaction, but both still turn on Spring's transaction synchronization for the method, and Spring binds one `EntityManager` to it, which keeps its connection until the method returns. Lazy fetching can't help, because the query has already fetched the real connection. According to the [`SUPPORTS` documentation](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Propagation.html), the same resources are shared for the entire scope. With no annotation at all, the same method held nothing. Spring Data repository calls behave differently again, because each repository method runs in its own short transaction (read-only for the query methods) and gives the connection back when it commits. `repo.count()` followed by the call held nothing under `SUPPORTS`, `NOT_SUPPORTED` or no annotation.

A suspended transaction keeps its connection. An outer `@Transactional` method that had run a query and then called a `NOT_SUPPORTED` method still held its connection during that call, with lazy fetching too. With `REQUIRES_NEW` instead, the inner transaction took a second connection while the outer one kept its first, so one request held two. The [Spring documentation](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html) warns that this can exhaust the pool.

Open session in view keeps one `EntityManager` open for the whole request, and Hibernate holds its connection from the first query until the response is written. With it on, every method that queried and then made the call held a connection, transaction or not. It's [on by default](https://docs.spring.io/spring-boot/reference/data/sql.html) for Spring Boot web apps, and Spring Boot logs a warning about it at startup.

## What I'd do

If a transactional method makes a slow call on some paths, and Spring Boot creates your pool, turn on `spring.datasource.connection-fetch=lazy`, on 4.1.3 or later if you use JMX. Check your error handling for the later pool timeout. Then turn off open session in view, and look at the methods that query before a slow call. They need the call moved or the transaction narrowed, and the flag doesn't change that.

## Caveats

One small app on one machine (Docker Desktop on an arm64 Mac), PostgreSQL only, one fixed downstream latency, a pool of 10, and three rounds. The rates scale with the pool size and the latency, so read the breakpoints as a ratio, not as numbers to expect in production.

k6 sent requests at perfectly even intervals with a fixed route pattern. Real traffic is burstier, so waits and failures start somewhat below these rates, and the cliff is less sharp. With a 30-second step and a 2-second timeout, a step also has to be a little above capacity before 1% of its requests time out.

The load test ran with open session in view off. With it on, the checks show the downstream path holding the same connections (one with eager fetching, none with lazy), but the database path would keep its connection a little longer, until the response is written. I'd expect similar breakpoints, and I didn't measure them. All runs used platform threads. Tomcat's 200 threads were never the limit at a breakpoint, though past it they nearly were, with about 190 busy at 90 req/s and half the requests downstream. Virtual threads wouldn't move the breakpoint, but past it more requests would reach the pool and time out, where platform threads would leave them waiting for a Tomcat thread.

## Method and data

- The app, the WireMock stub, the k6 script and the drivers are in [`benchmarks/tx-pool`](https://github.com/diegourban/diegourban.github.io/tree/master/benchmarks/tx-pool). The app sets `spring.jpa.open-in-view=false`, the pool size and the timeout, and declares its own `JpaTransactionManager` next to a JDBC one for the checks. The open session in view default (`true`) and its startup warning are in the configuration metadata and classes of `spring-boot-jpa` 4.1.2.
- `checks.py` runs the single-request checks, three repeats each, and the pool-of-1 contention check (`checks.json`). `bench.py` runs the load steps (`results.jsonl`).
- Latency is k6's p95 per route and step. Every chart and table value is the median of the three rounds. The breakpoints were the same step in every round, while the failure rate at a failing step varied (2.6% to 4.2% at 50% and 70 req/s). A request counts as failed when it returns an error or k6 couldn't send it on time; none was dropped.
- The pool figures are instantaneous samples from Actuator every half second, so short waits between samples aren't counted.
- The comparisons between setups pair runs by share and round (9 pairs). Their shift is the [Hodges-Lehmann estimate](https://doi.org/10.1214/aoms/1177704172) of the per-run median difference, its 95% interval a [bootstrap](https://doi.org/10.1214/aos/1176344552) (10,000 resamples, fixed seed), with the same code as the earlier posts. `analyze.py` computes those and the capacity model (`analysis.json`), and `make_charts.py` every number in the charts and tables.
