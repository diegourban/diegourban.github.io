---
title: "AppCDS on JDK 25: 0.8 to 1 second off Spring Boot startup"
date: 2026-10-26
draft: false
description: "AppCDS took 0.8 to 1 second off Spring Boot 4 startup on JDK 25. How to train the archive in your image, what it costs, and the settings that make it stop loading."
tags: ["java", "jvm", "cds", "appcds", "spring-boot", "performance", "startup"]
ShowToc: true
ai_disclosure: ai-generated
cover:
  image: cover.png
  relative: true
  alt: "Startup change with AppCDS vs default CDS, per GC: about 800 ms faster on a small Spring Boot app, about one second on a heavier one"
  hiddenInSingle: true
  hiddenInList: true
---

If you train a class-data archive for your own Spring Boot app, startup gets 0.8 to 1 second faster. I measured 784 to 828 ms on a small app across four garbage collectors, and 1,001 ms on a heavier one with Postgres, Flyway and 80 JPA entities. You pay for it with one training run in the image build and an archive of 87 to 104 MB.

Last time I removed a stray `-Xshare:off` and got back [a few tens of milliseconds](/posts/default-cds/), because the JDK's default archive only holds the JDK's own classes. AppCDS goes further and archives yours, as a second layer on top of the JDK's archive.

## From the JDK's classes to yours

With default CDS, about 1,400 classes come pre-parsed from the archive. The JVM loads everything else from the JARs and processes it again on every start. An AppCDS archive is trained on your application, so it holds almost everything Spring loads while it builds the application context.

{{< chart "class-coverage" >}}
Up to Spring's context refresh, AppCDS serves 97% of the small app's classes and 95% of the heavy app's, against 8 to 9% with the default archive. The refresh-time counts come from `-Xlog:class+load`, with ZGC. By the time the app reports ready it has loaded more (about 16,000 and 19,400 classes, by the actuator's class counter), so at readiness the archive covers at least 91% in both apps.
{{< /chart >}}

## What I measured

I didn't need a new benchmark for this. The one from the previous post already ran AppCDS as one of its variants: Spring Boot 4.1 on JDK 25 (Temurin 25.0.3, `gcr.io/distroless/java25-debian13`), a container limited to 2 CPUs and 1 GB, and ten rounds that started every variant once in random order. That means each AppCDS boot has a default CDS boot from the same round to be compared with. I compare against default CDS rather than `-Xshare:off` because if you followed the last post, that's what you're running now.

{{< chart "paired-shift" >}}
AppCDS was faster in all 50 pairs, and every 95% interval ends at least three quarters of a second below zero. The shift barely changes between garbage collectors.
{{< /chart >}}

{{< results-table >}}

The heavy app gained a bit more time than the small one, roughly in line with how many more classes it archived. Work it out per class and both apps land on about 62 µs per extra archived class (13,247 extra classes for 828 ms, 16,277 for 1,001 ms, both on ZGC). The heavy app's whole startup takes 2.3 times as long (4.50 s against 1.94 s with default CDS), so a similar saving is 22% of its startup and 43% of the small app's. My guess is that the rest is mostly Postgres, Flyway and Hibernate work that class archives don't touch, but I didn't break it down.

## Memory

I wouldn't count on saving memory. Metaspace drops from about 72 MB to 11 MB on the small app, but just like with default CDS that's mostly accounting. The archived classes move into the mapped archive and show up as 15 to 18 MB more file-backed memory. Total resident memory, sampled a second after the app reported ready, went down by 13 to 14 MB with Parallel and Serial and didn't change measurably with G1 or on the heavy app. With ZGC it went up by about 15 MB, and that rise comes from the heap. ZGC keeps its heap in shared memory, and when I sampled, the AppCDS runs happened to have more of it resident. The heavy app, also on ZGC, didn't show it. Treat those numbers with care, because AppCDS reaches readiness 0.8 to 1 second earlier and the two variants were sampled at different points in their warm-up. If you want to know when the file-backed part can be shared between JVMs, [the previous post](/posts/default-cds/#where-the-memory-goes) covers it.

## How to build it

Train the archive in the image build, with every JVM option the container will run with. I put those options in the image as `JAVA_TOOL_OPTIONS`, so the training run and the container pick up the same ones. Spring Boot's extracted layout keeps the classpath stable:

```bash
java -Djarmode=tools -jar app.jar extract --destination ext
```

```dockerfile
FROM gcr.io/distroless/java25-debian13:latest
WORKDIR /app
COPY ext/ /app/
ENV JAVA_TOOL_OPTIONS="-XX:+UseZGC"
RUN ["java", "-XX:ArchiveClassesAtExit=/app/app.jsa", "-Dspring.context.exit=onRefresh", "-jar", "/app/app.jar"]
ENTRYPOINT ["java", "-XX:SharedArchiveFile=/app/app.jsa", "-jar", "/app/app.jar"]
```

I adapted this from the benchmark's [Dockerfile](https://github.com/diegourban/diegourban.github.io/blob/master/benchmarks/default-cds/app-src/Dockerfile.gcs) and tested it in [`recipe/`](https://github.com/diegourban/diegourban.github.io/tree/master/benchmarks/appcds/recipe), where 14,686 classes come from the archive as built.

If your Deployment sets its own `JAVA_TOOL_OPTIONS`, those replace the image's, and the two have to agree. Setting it to G1 dropped the count to 1,438 classes, and so did setting only `-Xmx512m`, because that replaced the image's `-XX:+UseZGC`. With `-XX:+UseZGC -Xmx512m` all 14,686 were still there.

`-Dspring.context.exit=onRefresh` stops the JVM once every bean exists, before the web server, the runners and the readiness events start, so the build doesn't hang on a running server. Anything that loads after that point, or on the first requests, isn't in the archive.

### If startup needs a database

The heavy app validates its Flyway migrations and Hibernate schema on start, so its training run needs a database too, and a plain `RUN` line in the Dockerfile can't reach one. I trained it with `docker run` against a Postgres container, wrote the archive to a mounted volume and copied it into the final image. In CI you'd run Postgres as a service container next to the build.

The final image also has to contain exactly the JAR files the archive was trained on. AppCDS checks each classpath entry's size and modification time, so copy the archive into an image built from the same layer instead of rebuilding it.

## When the archive stops loading

My notes from the first experiment said a mismatched archive fails silently. Measuring it properly for this post showed that the JVM does complain, in a place where it's easy to miss.

I trained an archive with ZGC and started the app with G1. It started normally, and only 1,438 of its 15,318 classes came from the archive instead of 14,686. The JDK's base layer loaded and the app's layer got dropped, so you're back to default CDS, and the only sign is a few `[warning][cds]` lines in the log.

The archive is tied to the memory layout it was trained with, and the JVM does print four lines about it, right at the top of the startup log:

```text
[warning][cds] Unable to use shared archive file.
The saved state of UseCompressedOops and UseCompressedClassPointers is different from runtime, CDS will be disabled.
[error  ][cds] An error has occurred while processing the shared archive file. Run with -Xlog:aot,cds for details.
[error  ][cds] Loading dynamic archive failed.
```

Two settings decide that layout, compressed oops and compact object headers. ZGC runs without compressed oops, and so does G1 with a heap above about 32 GB. Turning on `-XX:+UseCompactObjectHeaders` breaks the match the same way, with 1,438 classes again and a message about `UseCompactObjectHeaders`. A G1 archive keeps working under Parallel, since both use the same layout.

Once you pass `-XX:SharedArchiveFile` with ZGC, even a correctly matched archive prints two `[error][cds]` lines on every start (`Cannot use CDS heap data`), because ZGC can't use the JDK's archived heap objects on JDK 25. If every deploy logs CDS errors, a new one won't stand out. And `java -version` still ends with `sharing` as long as the JDK's default archive loaded, so it won't tell you either.

To catch the mismatch, I'd use one of these:

- Count the archived classes once: run with `-Xlog:class+load` and count the lines with `source: shared objects file`. With a working archive that's close to every class.
- Start with `-Xshare:on` in a CI smoke test, using the real `-jar` and the production flags. A matched archive starts the app, and a mismatched one makes the JVM refuse to start. Run the check against your app rather than `-version`, because `-version` fails even with a good archive when the classpath doesn't match.
- Alert on any `[warning][cds]` line in your logs rather than one exact message, since the wording depends on what didn't match.

`-XX:+AutoCreateSharedArchive` writes a fresh archive at JVM exit whenever the existing one is missing or doesn't match. The run after a change starts without AppCDS and pays for the dump at shutdown, the dump only happens on a clean exit, and the archive path has to outlive the process. That fits a long-lived host better than an immutable container image.

## What it costs

| | Small app | Heavy app |
|---|---|---|
| Archive size (added to the image) | 87 MB | 104 MB |
| Training run added to the build | 3.6 s (a plain boot takes 2.0 s) | 6.1 s, against an already migrated database |

On a fresh CI database, the heavy app's training also waits for Postgres and applies every migration. Neither timing covers pushing and pulling the bigger image layer.

The archive is also tied to the exact JDK build, the exact JAR files and the memory-layout flags. Change a dependency or bump the base image and you have to retrain. If the training run lives in the build, that happens on every build without you thinking about it, and that's the main reason I'd put it there. Pin the base image by digest if you want the JDK to change only when you decide.

## And the AOT cache?

JDK 24 added a successor, the AOT cache from Project Leyden ([JEP 483](https://openjdk.org/jeps/483)). It builds on CDS and also stores classes already loaded and linked, and JDK 25 adds a one-step `-XX:AOTCacheOutput` workflow ([JEP 514](https://openjdk.org/jeps/514)) and method profiles ([JEP 515](https://openjdk.org/jeps/515)). [Spring Boot recommends it](https://docs.spring.io/spring-boot/reference/packaging/aot-cache.html) on Java 25. With ZGC on JDK 25 the cache still loads classes, but it can't store heap objects (its creation log says `ZGC is not supported`). [JEP 516](https://openjdk.org/jeps/516), delivered in JDK 26, lifts that restriction. I'll measure AppCDS against the AOT cache, on each GC, in the next post.

## What I'd do

Spring Boot points to the AOT cache, and I haven't measured that yet. Until the next post does, AppCDS is what I've measured on every GC, so on JDK 25 I'd train an AppCDS archive in the image build with the exact production JVM options. Then I'd add a `-Xshare:on` smoke test to CI, so a mismatch breaks the build instead of costing you that second in production. The same train-in-the-build recipe carries over to the AOT cache with `-XX:AOTCacheOutput`.

## Caveats

These are the same demo apps as before, on one machine (Docker Desktop on an arm64 Mac), in one session, with ten paired boots per variant, and the heavy app only ran with ZGC. I measured startup and memory and nothing else, so measure your own service.

## Method and data

- The startup and memory numbers are the AppCDS variant of [the default-CDS benchmark](https://github.com/diegourban/diegourban.github.io/tree/master/benchmarks/default-cds): same runs, same pairing by round, the Hodges-Lehmann estimate with a bootstrap 95% CI (10,000 resamples, fixed seed) and exact Wilcoxon signed-rank tests (p = 0.002 in every setup, the smallest possible with ten pairs). Memory is sampled about a second after readiness. Numbers are rounded half away from zero.
- Class counts, the mismatch tests, the build cost and the Dockerfile recipe come from [`benchmarks/appcds`](https://github.com/diegourban/diegourban.github.io/tree/master/benchmarks/appcds): `measure_coverage.sh`, `measure_trap.sh`, `measure_build_cost.sh` and `recipe/test.sh` (its output is in `recipe/results.txt`). Build costs are medians of 5 runs (small app) and 3 runs (heavy app); each mismatch case is a single run. `measure_trap.sh` also records the JDK build (Temurin 25.0.3+9) and the `-Xshare:on -version` check, and `measure_aot_zgc.sh` records the AOT cache on ZGC (`aot_zgc.txt`). `make_charts.py` turns the results into the numbers in the charts and the table.

---

*Which JVM flags does your Deployment add that your image build never sees?*
