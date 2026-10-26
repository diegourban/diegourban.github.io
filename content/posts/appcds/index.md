---
title: "One training run, one second off Spring Boot startup: AppCDS on JDK 25"
date: 2026-10-26
draft: true
description: "AppCDS on JDK 25 took 0.8 to 1 second off Spring Boot 4 startup across four GCs. How to train the archive in your image, what it costs, and the flag mismatch that quietly throws it away."
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

Training a class-data archive for your own Spring Boot app takes 0.8 to 1 second off startup. On a small app that was 784 to 828 ms across four garbage collectors; on a heavier one with Postgres, Flyway and 80 JPA entities, it was 1,001 ms. The cost is one training run in the image build and an archive of about 90 to 100 MB.

This follows [the post about default CDS](/posts/default-cds/), where removing a stray `-Xshare:off` saved a fixed few tens of milliseconds. The JDK's default archive only covers the JDK's own classes. AppCDS archives yours, as a second layer on top of the JDK's archive.

## From the JDK's classes to yours

With default CDS, about 1,400 classes come pre-parsed from the archive. Everything else is loaded from the JARs and processed again on every start. An AppCDS archive is trained on your application, so it holds almost everything Spring loads while it builds the application context.

{{< chart "class-coverage" >}}
Up to Spring's context refresh, AppCDS serves 97% of the small app's classes and 95% of the heavy app's, against 8 to 9% with the default archive. By the time the app reports ready it has loaded more (about 16,000 and 19,400 classes), so at readiness the archive covers about 92% and 91%. Counted with `-Xlog:class+load`, with ZGC.
{{< /chart >}}

## What I measured

These numbers come from the same benchmark as the previous post: Spring Boot 4.1 on JDK 25 (Temurin 25.0.3, `gcr.io/distroless/java25-debian13`), a container limited to 2 CPUs and 1 GB, ten rounds that started every variant once in random order. AppCDS was one of those variants all along, so each AppCDS boot is compared with the default CDS boot from the same round. I picked default CDS as the baseline on purpose, because that's where you are if you followed the last post.

{{< chart "paired-shift" >}}
AppCDS was faster in all 50 pairs, and every 95% interval ends at least 756 ms below zero. The shift barely changes between garbage collectors.
{{< /chart >}}

{{< results-table >}}

The heavy app saved a bit more time than the small one, roughly in proportion to the classes it archived. Both apps saved about 62 µs per extra archived class (13,247 extra classes for 828 ms, 16,277 for 1,001 ms, both on ZGC). Everything else in the heavy app's startup takes 2.3 times longer. So the same per-class saving comes to 22% there and 43% on the small app. I'd guess that rest is mostly Postgres, Flyway and Hibernate work that class archives don't touch, but I didn't break it down.

## Memory

Don't expect a memory win. Metaspace drops from about 72 MB to 11 MB on the small app, but as with default CDS that's mostly accounting, because the archived classes move into the mapped archive and show up as 15 to 18 MB more file-backed memory. Total resident memory, sampled a second after the app reported ready, went down by 13 to 14 MB with Parallel and Serial, didn't change measurably with G1 or on the heavy app, and went up by about 15 MB with ZGC, whose heap counts as shared memory. AppCDS also reaches readiness 0.8 seconds earlier, so the two variants were sampled at different points in their warm-up. [The previous post](/posts/default-cds/#where-the-memory-goes) explains when the file-backed part can be shared between JVMs.

## How to build it

Train the archive in the image build, with every JVM option the container will run with. The simplest way to keep them identical is to put them in the image as `JAVA_TOOL_OPTIONS`, which reaches both the training run and the container. Spring Boot's extracted layout keeps the classpath stable:

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

It's adapted from the benchmark's [Dockerfile](https://github.com/diegourban/diegourban.github.io/blob/master/benchmarks/default-cds/app-src/Dockerfile.gcs), and [`recipe/`](https://github.com/diegourban/diegourban.github.io/tree/master/benchmarks/appcds/recipe) tests it. As built, 14,686 classes come from the archive.

Watch the Deployment, though. Its own `JAVA_TOOL_OPTIONS` replaces the image's, so the two have to match. When I set it to G1 the count fell to 1,438 classes, and when it only added `-Xmx512m` all 14,686 were still there.

`-Dspring.context.exit=onRefresh` stops the JVM once every bean exists, before the web server, the runners and the readiness events start, so the build doesn't hang on a running server. Classes that load after that point, or on the first requests, aren't in the archive.

### If startup needs a database

The heavy app validates its Flyway migrations and Hibernate schema on start, so the training run needs a database too, and a plain `RUN` line in the Dockerfile can't reach one. I trained it with `docker run` against a Postgres container, wrote the archive to a mounted volume and copied it into the final image. In CI that's a service container next to the build.

The final image has to contain exactly the JAR files the archive was trained on. AppCDS checks each classpath entry's size and modification time, so copy the archive into an image built from the same layer, not a rebuild of it.

## The flag that quietly throws it away

I trained an archive with ZGC and started the app with G1. The app started normally, and only 1,438 of its 15,313 classes came from the archive instead of 14,686. Only the JDK's base layer loaded and the app's layer was dropped, which leaves you with default CDS again.

An AppCDS archive is tied to the memory layout it was trained with, and the JVM says so in four lines at the very top of the startup log:

```text
[warning][cds] Unable to use shared archive file.
The saved state of UseCompressedOops and UseCompressedClassPointers is different from runtime, CDS will be disabled.
[error  ][cds] An error has occurred while processing the shared archive file. Run with -Xlog:aot,cds for details.
[error  ][cds] Loading dynamic archive failed.
```

Two settings decide that layout, compressed oops and compact object headers. ZGC runs without compressed oops, and so does G1 with a heap above about 32 GB. Turning on `-XX:+UseCompactObjectHeaders` breaks the match the same way, with 1,438 classes again and a message about `UseCompactObjectHeaders`. A G1 archive keeps working under Parallel, since both use the same layout.

On ZGC those lines are also easy to dismiss. Once you pass `-XX:SharedArchiveFile` with ZGC, even a correctly matched archive prints two `[error][cds]` lines on every start (`Cannot use CDS heap data`), because ZGC can't use the JDK's archived heap objects on JDK 25. After a few deploys with those lines in every log, CDS errors stop looking unusual. `java -version` won't help you either, because it still ends with `sharing` as long as the JDK's default archive loaded.

You can catch the mismatch in three ways.

- Count the archived classes once: run with `-Xlog:class+load` and count the lines with `source: shared objects file`. With a working archive that's close to every class.
- Start with `-Xshare:on` in a CI smoke test, using the real `-jar` and the production flags. A matched archive starts the app, and a mismatched one makes the JVM refuse to start. Don't run that check with `-version` instead of your app, since it fails even with a good archive when the classpath doesn't match.
- Alert on `Unable to use shared archive file` in your logs.

There's also `-XX:+AutoCreateSharedArchive`, which regenerates the archive whenever it doesn't match. It needs a writable archive path and makes the first start after a change slow, so it suits a long-lived host better than an immutable container image.

## What it costs

| | Small app | Heavy app |
|---|---|---|
| Archive size (added to the image) | 87 MB | 104 MB |
| Training run added to the build | 3.6 s (a plain boot takes 2.0 s) | 6.1 s, against an already migrated database |

On a fresh CI database, the heavy app's training also waits for Postgres and applies every migration. Neither timing covers moving the bigger image layer through your registry.

The archive is also tied to the exact JDK build, the exact JAR files and the memory-layout flags. Change a dependency or bump the base image and you retrain. With the training run in the build, that happens on every build anyway, which is the main reason to put it there. Pin the base image by digest if you want the JDK to change only when you choose.

## And the AOT cache?

JDK 24 added a successor, the AOT cache from Project Leyden ([JEP 483](https://openjdk.org/jeps/483)). It builds on CDS and also stores classes already loaded and linked; JDK 25 adds a one-step `-XX:AOTCacheOutput` workflow ([JEP 514](https://openjdk.org/jeps/514)) and method profiles ([JEP 515](https://openjdk.org/jeps/515)). [Spring Boot recommends it](https://docs.spring.io/spring-boot/reference/packaging/aot-cache.html) on Java 25. With ZGC on JDK 25 the cache still loads classes, but it can't store heap objects (its creation log says `ZGC is not supported`); [JEP 516](https://openjdk.org/jeps/516), delivered in JDK 26, lifts that restriction. Measuring AppCDS against the AOT cache, on each GC, is the next post.

## What I'd do

On JDK 25, train an AppCDS archive in the image build with the exact production JVM options, and add a `-Xshare:on` smoke test to CI so a flag mismatch fails the build before it costs you the second in production.

## Caveats

These are the same demo apps as before, on one machine (Docker Desktop on an arm64 Mac), in one session, with ten paired boots per variant, and the heavy app ran with ZGC only. I measured startup and memory, nothing else. Measure your own service.

## Method and data

- The startup and memory numbers are the AppCDS variant of [the default-CDS benchmark](https://github.com/diegourban/diegourban.github.io/tree/master/benchmarks/default-cds): same runs, same pairing by round, the Hodges-Lehmann estimate with a bootstrap 95% CI (10,000 resamples, fixed seed) and exact Wilcoxon signed-rank tests (p = 0.002 in every setup, the smallest possible with ten pairs). Memory is sampled about a second after readiness. Numbers are rounded half away from zero.
- Class counts, the flag-mismatch tests, the build cost and the Dockerfile recipe come from [`benchmarks/appcds`](https://github.com/diegourban/diegourban.github.io/tree/master/benchmarks/appcds): `measure_coverage.sh`, `measure_trap.sh`, `measure_build_cost.sh` and `recipe/test.sh`. `make_charts.py` turns the results into the numbers in the charts and the table.

---

*Which JVM flags does your Deployment add that your image build never sees?*
