---
title: "JDK 25's AOT cache vs AppCDS: faster startup, and a second off warm-up"
date: 2026-10-27
draft: false
description: "Trained with a few thousand requests, the AOT cache started a Spring Boot 4 app about 0.2 s faster than AppCDS on G1, Parallel and Serial and took a second off its first 2,000 requests. ZGC needed JDK 26 for the same."
tags: ["java", "jvm", "aot", "appcds", "leyden", "spring-boot", "performance", "startup"]
ShowToc: true
ai_disclosure: ai-generated
references:
  - author: "OpenJDK"
    title: "JEP 483: Ahead-of-Time Class Loading & Linking"
    venue: "Delivered in JDK 24"
    year: 2025
    url: "https://openjdk.org/jeps/483"
  - author: "OpenJDK"
    title: "JEP 514: Ahead-of-Time Command-Line Ergonomics"
    venue: "Delivered in JDK 25"
    year: 2025
    url: "https://openjdk.org/jeps/514"
  - author: "OpenJDK"
    title: "JEP 515: Ahead-of-Time Method Profiling"
    venue: "Delivered in JDK 25"
    year: 2025
    url: "https://openjdk.org/jeps/515"
  - author: "OpenJDK"
    title: "JEP 516: Ahead-of-Time Object Caching with Any GC"
    venue: "Delivered in JDK 26"
    year: 2026
    url: "https://openjdk.org/jeps/516"
  - author: "OpenJDK"
    title: "JEP 310: Application Class-Data Sharing"
    venue: "Delivered in JDK 10"
    year: 2018
    url: "https://openjdk.org/jeps/310"
  - author: "OpenJDK"
    title: "JEP 350: Dynamic CDS Archives"
    venue: "Delivered in JDK 13"
    year: 2019
    url: "https://openjdk.org/jeps/350"
  - author: "Oracle"
    title: "The java Command"
    venue: "Java SE 25 Tool Reference"
    year: 2025
    url: "https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html"
  - author: "Spring Boot"
    title: "AOT Cache"
    venue: "Spring Boot Reference Documentation"
    url: "https://docs.spring.io/spring-boot/reference/packaging/aot-cache.html"
  - author: "Parlog, N., Chalupa, M. and Rose, J."
    title: "AOT Caching - Netflix' Practice vs OpenJDK's Theory"
    venue: "Inside Java Podcast (show notes)"
    year: 2026
    url: "https://insidejava.libsyn.com/website/aot-caching-netflix-practice-vs-openjdks-theory-io"
  - author: "OpenJDK"
    title: "JDK-8381222: Running a custom runtime image with AOTCache fails with \"Unexpected exception\" [jdk26]"
    venue: "JDK Bug System, closed as Won't Fix"
    year: 2026
    url: "https://bugs.openjdk.org/browse/JDK-8381222"
    checked: "by hand"
  - author: "OpenJDK"
    title: "JDK-8373392: Replace CDS object subgraphs with @AOTSafeClassInitializer"
    venue: "JDK Bug System, fixed in JDK 27"
    year: 2025
    url: "https://bugs.openjdk.org/browse/JDK-8373392"
    checked: "by hand"
  - author: "Helidon"
    title: "Java 26 - Error occurred during initialization of VM when using AOTCache with custom runtime image (issue #11346)"
    venue: "GitHub"
    year: 2026
    url: "https://github.com/helidon-io/helidon/issues/11346"
  - author: "Wilcoxon, F."
    title: "Individual Comparisons by Ranking Methods"
    venue: "Biometrics Bulletin 1(6), 80–83"
    year: 1945
    url: "https://doi.org/10.2307/3001968"
    checked: "by hand"
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
---

On JDK 25 with G1, Parallel or Serial, the AOT cache started my Spring Boot app 205 to 220 ms faster than AppCDS, and its first 2,000 requests took 0.9 to 1.06 seconds less. That warm-up gain needs a training run that serves requests. A cache trained only up to Spring's context refresh, which is what Spring's own recipe does, starts at least as fast and then warms up no faster than AppCDS. ZGC is the odd one out on JDK 25, where the small app's cache lost to AppCDS by 70 ms and the heavy app's won by 129 ms. On JDK 26 the cache beat AppCDS on ZGC by 232 ms and took a second off the requests.

[The last post](/posts/appcds/) ended with AppCDS as the safe choice on JDK 25 because I hadn't measured the AOT cache yet, so this post puts the two side by side.

## What the AOT cache adds

[AppCDS](https://openjdk.org/jeps/310) archives your classes after they're parsed, as a [second layer](https://openjdk.org/jeps/350) on top of the JDK's archive. The AOT cache from Project Leyden ([JEP 483](https://openjdk.org/jeps/483)) uses the same mechanism and keeps them loaded and linked too. It replaces the JDK's default archive rather than layering on it. JDK 25 added a one-step way to create it, `-XX:AOTCacheOutput` ([JEP 514](https://openjdk.org/jeps/514)), and method profiles from the training run ([JEP 515](https://openjdk.org/jeps/515)), so the JIT can compile hot methods early instead of profiling them all over again.

With ZGC on JDK 25, the cache can't hold heap objects, and without them HotSpot also skips the class linking, the profiles and the cached code. Booted with `-Xlog:aot=info`, a G1 cache prints `Using AOT-linked classes: true` and a ZGC cache prints `Using AOT-linked classes: false`. [JEP 516](https://openjdk.org/jeps/516) brings all of that back for ZGC in JDK 26.

Spring Boot [recommends the AOT cache](https://docs.spring.io/spring-boot/reference/packaging/aot-cache.html) on Java 25. Its documented training run stops at the context refresh (`-Dspring.context.exit=onRefresh`), and in my numbers that one flag cost the whole warm-up gain.

## What I measured

Same two apps as before: a small Spring Boot 4.1 app, and a heavier one with Postgres, Flyway and 80 JPA entities, on JDK 25 (Temurin 25.0.3 on `gcr.io/distroless/java25-debian13`) in containers limited to 2 CPUs and 1 GB, with ten rounds that start every variant once in random order. I re-ran default CDS and AppCDS in the same session instead of reusing old numbers, so every comparison is between boots from the same round.

Two things changed from the earlier posts:

- After the app reports ready, the benchmark sends it 2,000 requests in a row over one connection and times each one. The mix stays the same size the whole time, with GETs of a fixed list of 50 items alternating with POSTs that fail validation. So a request costs the same at the end as at the start, apart from the JVM warming up.
- Every archive and cache is trained with that same kind of scripted traffic. The app starts, serves 5,000 requests and shuts down through Spring's shutdown endpoint, so the JVM exits and writes the file. In the last post AppCDS was trained only up to the context refresh, which is why it costs more here.

{{< chart "paired-shift" "startup" >}}
The AOT cache won every round with G1, Parallel and Serial. On ZGC with JDK 25 the small app lost every round and the heavy app won nine of ten.
{{< /chart >}}

{{< results-table >}}

With G1 the median startup went from 1.91 s with default CDS to 1.10 s with AppCDS and 0.88 s with the AOT cache. AppCDS against default CDS came out at 0.79 to 0.84 seconds again, which matches the last post.

The heavy app ran only with ZGC, where it gained 129 ms from the AOT cache while the small app lost 70 ms. I didn't find out why the two apps went different ways.

## Warm-up is where the profiles show

The startup gain is the smaller half, and the bigger one comes in the requests that follow, where the method profiles in the cache let the JIT compile the request path early.

{{< chart "lines" "warmup" >}}
Orange is the AOT cache trained with requests. Dashed green is a cache trained only up to the context refresh. It has the startup classes but neither the classes nor the profiles of the request path, so it warms up like AppCDS.
{{< /chart >}}

With G1, between the 201st and the 500th request a GET took a median of 1.69 ms with the trained AOT cache against 2.39 ms with AppCDS, and a failing POST 0.72 ms against 1.22 ms. Paired by round, the AOT cache saved 890 ms over the 2,000 requests with G1, 1,007 ms with Parallel and 1,057 ms with Serial, on top of starting faster. Those numbers are a floor, because the gap was still open at the 2,000th request. The very first request took 16.9 ms, against 22.7 ms with AppCDS and 72.2 ms with default CDS.

On ZGC with JDK 25 the small app showed no warm-up difference that ten rounds could pin down (−53 ms, 95% CI −265 to +63 ms, where the smallest effect this design reliably detects is about 230 ms), which fits a cache without profiles. The heavy app, also on ZGC, saved 99 ms over its 2,000 requests.

## ZGC needs JDK 26

To see what JEP 516 changes, I ran the small app with ZGC on full Temurin JDK images, 25.0.4.1 and 26.0.2.1, in a second session. Default CDS, AppCDS and the AOT cache ran on each JDK, all six in the same ten rounds.

| ZGC, small app | Startup | 2,000 requests |
|---|---|---|
| JDK 25: AOT cache vs AppCDS | +96 ms (+68 to +118), 0/10 faster | −95 ms (−185 to −26) |
| JDK 26: AOT cache vs AppCDS | −232 ms (−250 to −214), 10/10 faster | −1,010 ms (−1,074 to −965) |
| AppCDS: JDK 26 vs JDK 25 | −4 ms (−36 to +16) | −38 ms (−191 to +40) |

AppCDS and default CDS ran the same on both JDKs, so the gain on JDK 26 belongs to the cache. Its median startup dropped from 1.24 s to 0.91 s, against 1.14 s for AppCDS on either JDK, and the cache grew from 109 MB to 142 MB with the linked classes and profiles back in it (`-Xlog:aot` now says `Using AOT-linked classes: true`).

## How much training is enough

To find out how long a training run needs to be, I trained G1 caches with 0, 200, 1,000, 5,000 and 20,000 requests and compared each with the 5,000-request cache.

{{< chart "paired-shift" "training" >}}
Startup stayed within 25 ms of the 5,000-request cache, and the cache trained up to the refresh was even 25 ms faster. What changed was the time for the 2,000 requests after startup.
{{< /chart >}}

Without any requests, the cache gave up 960 ms of the warm-up gain. After 200 requests it was still 753 ms behind, and after 1,000 it was 447 ms behind. At 20,000 requests it was no better than at 5,000 (+32 ms, CI −77 to +136 ms, where ten rounds detect about 150 ms). So 5,000 requests were enough for this app and 1,000 weren't. I didn't test anything in between, and each point is a single cache, so a second training run at the same length could land somewhere else.

To see when a training run has seen enough, I logged new classes and JIT compilations during a 20,000-request training run.

{{< chart "lines" "signal" >}}
The x-axis is logarithmic. New classes almost stop at once; JIT compilations take a few thousand requests to die down.
{{< /chart >}}

The first 100 requests loaded 818 new classes, and after about 500 requests only 14 more appeared. Compilations fell off more slowly, with 61% of them done by request 1,000 and 90% by 5,000. That fits the curve above better than class loading does, but one logged run and one cache per length can't prove the link. Treat "watch the compilations" as a starting point and measure your own plateau. `-Xlog:jit+compilation=debug` logs each compilation with a timestamp.

That rule is for this app and this traffic, which repeats a handful of requests. Real traffic has paths that only run once a day or once a month, like a nightly job, an admin screen or an error handler, and the compilation count says nothing about paths the training run hasn't seen.

## How to train it

This is the shape of the training run I used, with Spring Boot's actuator doing the shutdown:

```bash
java -XX:AOTCacheOutput=app.aot \
     -Dmanagement.endpoints.web.exposure.include=health,shutdown \
     -Dmanagement.endpoint.shutdown.access=unrestricted \
     -jar app.jar &
# wait until /actuator/health is UP, send your requests, then:
curl -X POST localhost:8080/actuator/shutdown
```

Open the shutdown endpoint only for the training run, never in the image's `ENTRYPOINT`, because it's unauthenticated. For production, start the app with `-XX:AOTCache=app.aot` and the same JVM options. The cache is written by a second JVM that starts as the training run exits, and per [JEP 514](https://openjdk.org/jeps/514) it uses as much heap again, so give the training container that memory. `JDK_AOT_VM_OPTIONS` sets options for that second JVM alone.

## Where to train it

There are three places to run the training:

| Where | What it trains on | What it costs you |
|---|---|---|
| Image build, up to the context refresh | Startup only | Nothing extra, but no request-path profiles, so about a second of warm-up left on the table here |
| Image build, with a scripted workload | Traffic you write | A test setup in the build, and for the heavy app a database in CI |
| A canary in production | Real traffic | Somewhere to store the cache and hand it to the next pods |

The third is Netflix's approach, as described in [an Inside Java Podcast episode](https://insidejava.libsyn.com/website/aot-caching-netflix-practice-vs-openjdks-theory-io) with Martin Chalupa from Netflix and John Rose, the lead of Project Leyden. A canary deployment of the new version collects the training data, and the rest of the deployment uses the cache built from it. Going by what I measured and by [JEP 483](https://openjdk.org/jeps/483)'s consistency rules, a canary setup has to guarantee a few things:

- The canary runs the same image and the same JVM flags as the main pods, including anything the Deployment adds.
- A new image needs its own canary run, since a cache belongs to one JDK release and one set of JARs. Touching the JAR file made mine stale.
- Between the canary and the next start, the cache has to live somewhere (a volume, object storage) and the main pods have to fetch it before their JVMs start.
- Only the pipeline should be able to write where the cache is stored, because the JVM maps that file into every pod.
- The canary has to be stopped at some point and given time to finish. A training run is the application running its workload "and then exit", as JEP 483 puts it, and the second JVM that writes the cache needs its memory and a grace period long enough to complete.

On JDK 25 caches also don't build up over several runs, because a training run can't start from an existing cache. Give it both `-XX:AOTCache` and `-XX:AOTCacheOutput` and the JVM stops with `Only one of AOTCache or AOTCacheOutput can be specified`. JEP 514 mentions that reading an existing cache during training may come later. Until then, one canary run has to see a full traffic cycle on its own, often a whole day, or the paths it missed start cold.

## When the cache doesn't load

On JDK 25 the app started whether the cache file was there or not. With the default `-XX:AOTMode=auto`, a missing, empty, truncated or stale cache (the JAR file had a new timestamp), and a G1 cache started with ZGC, all started the app without the cache. Each printed three or four `[warning]` and `[error][aot]` lines and loaded no shared classes at all, not even from the JDK's default archive. That makes a broken AOT cache worse than a broken AppCDS archive, which falls back to the default archive. With `-XX:AOTMode=on` the same cases stopped the JVM within a third of a second.

The [`java` command reference](https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html) recommends the same split. It says `auto` "is the recommended mode for production environments", and that `on` "should be used only as a 'fail-fast' debugging aid". So leave production on `auto`, and put `on` in a CI smoke test that checks the cache loads. On a running JVM, `-Xlog:aot=info` prints the `Using AOT-linked classes` line, and `-Xlog:class+load` lines ending in `source: shared objects file` count the cached classes.

Two cases don't fall back cleanly.

- **The JDK 26 JRE image.** With the Temurin JRE image on JDK 26, every AOT cache I built made the JVM refuse to start, with G1 and with ZGC, even under `auto`:

  ```text
  [error][aot] java.lang.InternalError: (no message)
  Error occurred during initialization of VM
  Unexpected exception when loading aot-linked classes.
  ```

  The cause is [JDK-8381222](https://bugs.openjdk.org/browse/JDK-8381222). On JDK 26 an AOT cache fails on a jlink runtime image that has no module loaded by the application class loader, and the Temurin JRE image (26.0.2.1 here) is built that way. OpenJDK closed the bug as Won't Fix for JDK 26, and the underlying change ([JDK-8373392](https://bugs.openjdk.org/browse/JDK-8373392)) is in JDK 27. With the full JDK image everything worked. [The Helidon issue](https://github.com/helidon-io/helidon/issues/11346) where I found the bug lists the other ways out, adding a module such as `jdk.compiler` to your own jlink image, or using an AppCDS archive on JDK 26. JDK 25 wasn't affected.
- **A Java agent added only at runtime.** Starting the G1 cache with the OpenTelemetry agent attached, when it was trained without it, loaded none of the 19,108 classes from the cache. [JEP 483](https://openjdk.org/jeps/483) lists class-rewriting agents among the things that make the JVM ignore the cache, so if you run an agent, train with the same agent attached and check that the cache loads. I haven't tested training with an agent.

Adding `-Xmx512m` didn't stop the cache from loading, and neither did starting the G1 cache with Serial, since JEP 483 allows a different GC between training and production. The Deployment override from [the last post](/posts/appcds/#when-the-archive-stops-loading) only bites here when it changes the memory layout, such as a switch to ZGC, a heap above about 32 GB or compact object headers.

Every start of a G1, Parallel or Serial cache also logs two warnings, `Saved blob's name ... is different from the expected name` and `Failed to link AdapterHandlerEntry`. With `-Xlog:aot+codecache+init=info` the JVM reports 740 cached adapter entries (small call stubs, not compiled methods), and one of them didn't match and was regenerated while the rest of the cache loaded. I'd leave both lines out of any alert on cache warnings.

## What it costs

| | AppCDS | AOT cache |
|---|---|---|
| File size, G1, trained on 5,000 requests | 98 MB | 130 MB |
| Training run in the build | 11.7 s | 15.1 s |
| Trained only up to the context refresh | | 112 MB, 6.1 s |
| Trained on 20,000 requests | | 29.5 s |

For ZGC the cache was 107 MB on the distroless JDK 25 image, and 109 MB on Temurin 25 against 142 MB on Temurin 26. The heavy app's was 120 MB. Total memory after the 2,000 requests didn't move in a consistent direction, staying between −4 and +1 MB on G1, Parallel and Serial, where ten rounds could detect about 6 to 10 MB. On ZGC it was 12 MB higher, which is heap residency rather than the cache.

## What I'd do

On JDK 25 with G1, Parallel or Serial, I'd switch from AppCDS to the AOT cache and train it with requests. Don't stop the training at the context refresh the way Spring's recipe does: here that kept the startup gain and lost the warm-up one. Scripted traffic in the build gets most of it back, and 5,000 requests were enough for this app. If you can run a canary on real traffic, that's better, as long as it covers a full traffic cycle and runs the exact production image and flags. On ZGC I'd move to JDK 26 for the AOT cache, using a full JDK image or adding a module to a custom one. With ZGC on JDK 25, measure your own app, since mine went both ways.

## Caveats

These are demo apps on one machine (Docker Desktop on an arm64 Mac), with ten rounds per variant. The heavy app only ran with ZGC. Warm-up traffic came from one client sending requests in sequence, so this isn't a load test. Each archive and cache was trained once, so the intervals cover measuring a cache, not training it again. One of the 230 runs in the main session failed before the JVM started (Docker couldn't create the container, exit 125), so AppCDS on Serial has nine pairs instead of ten. ZGC keeps its heap in shared memory, so its resident memory isn't comparable with the other collectors.

## Method and data

- Startup is Spring's `process running for`. The warm-up numbers are the latency of each of the 2,000 requests, timed from the host and reported per request type. Memory is sampled from `/proc/1/status` one second after readiness and again after the requests.
- Each pair is compared within a round. The shift is the [Hodges-Lehmann estimate](https://doi.org/10.1214/aoms/1177704172) of the paired differences, its 95% interval a [bootstrap](https://doi.org/10.1214/aos/1176344552) of that estimate (10,000 resamples, fixed seed), and p comes from exact [Wilcoxon signed-rank tests](https://doi.org/10.2307/3001968). For the smallest detectable effect I used 2.8 times the standard deviation of the paired differences over the square root of the number of pairs. Numbers are rounded half away from zero, and sizes are decimal MB.
- Scripts, raw results and the analysis are in [`benchmarks/aot-cache`](https://github.com/diegourban/diegourban.github.io/tree/master/benchmarks/aot-cache): `train.py` builds every archive and cache (sizes and training times in `training.json`) and logs the training signal (`signal.json`), `bench.py` runs the rounds (`results.jsonl` for the main session, `results_jdk.jsonl` for the JDK 25 and 26 session), `analyze.py` and `make_charts.py` compute every number in the charts and tables, and `measure_oneoffs.py` records the broken-cache, mismatch, agent and extend checks with their logs (`oneoffs.json`). I added the retry on `docker run` to `bench.py` during the main session, after that failed run; the JDK session ran with it.
