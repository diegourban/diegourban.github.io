---
title: "Is anything switching off your JVM's free startup optimisation?"
date: 2026-10-18
draft: false
description: "Removing a leftover -Xshare:off: what the default CDS archive does and doesn't buy a Spring Boot 4 app on JDK 25, measured across four GCs."
tags: ["java", "jvm", "cds", "spring-boot", "performance", "startup"]
ShowToc: true
ai_disclosure: ai-generated
cover:
  image: cover.png
  relative: true
  alt: "Startup change after removing -Xshare:off, per GC: about 2% faster, 5–7 MB less private memory"
  hiddenInSingle: true
  hiddenInList: true
references:
  - author: "OpenJDK"
    title: "JEP 341: Default CDS Archives"
    venue: "Delivered in JDK 12"
    year: 2019
    url: "https://openjdk.org/jeps/341"
  - author: "OpenJDK"
    title: "JEP 310: Application Class-Data Sharing"
    venue: "Delivered in JDK 10"
    year: 2018
    url: "https://openjdk.org/jeps/310"
  - author: "OpenJDK"
    title: "JEP 516: Ahead-of-Time Object Caching with Any GC"
    venue: "Delivered in JDK 26"
    year: 2026
    url: "https://openjdk.org/jeps/516"
  - author: "OpenJDK"
    title: "JEP 519: Compact Object Headers"
    venue: "Delivered in JDK 25"
    year: 2025
    url: "https://openjdk.org/jeps/519"
  - author: "Oracle"
    title: "Class Data Sharing"
    venue: "Java SE 25 HotSpot Virtual Machine Guide"
    year: 2025
    url: "https://docs.oracle.com/en/java/javase/25/vm/class-data-sharing.html"
  - author: "Oracle"
    title: "The java Command"
    venue: "Java SE 25 Tool Reference"
    year: 2025
    url: "https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html"
  - author: "Wilcoxon, F."
    title: "Individual Comparisons by Ranking Methods"
    venue: "Biometrics Bulletin 1(6), 80–83"
    year: 1945
    url: "https://doi.org/10.2307/3001968"
  - author: "Hodges, J. L. and Lehmann, E. L."
    title: "Estimates of Location Based on Rank Tests"
    venue: "The Annals of Mathematical Statistics 34(2), 598–611"
    year: 1963
    url: "https://doi.org/10.1214/aoms/1177704172"
  - author: "Efron, B."
    title: "Bootstrap Methods: Another Look at the Jackknife"
    venue: "The Annals of Statistics 7(1), 1–26"
    year: 1979
    url: "https://doi.org/10.1214/aos/1176344552"
---

Deleting a leftover `-Xshare:off` from our JVM flags made startup only slightly faster. It also showed why: default CDS saves a fixed few tens of milliseconds of JVM startup, and your startup time is mostly your own classes.

Since JDK 12 ([JEP 341](https://openjdk.org/jeps/341)), every JDK ships **CDS (Class Data Sharing)** archives in `lib/server/*.jsa`, holding the JDK's core classes already parsed and laid out. At startup the JVM [memory-maps](https://docs.oracle.com/en/java/javase/25/vm/class-data-sharing.html) one of them instead of loading those classes from scratch, and it does this by default with nothing to configure.

Several of our services had `-Xshare:off` in their flags, which switches all of that off; it came from an old base image, and nobody had written down why. I deleted it and expected a noticeably faster startup, but didn't get one.

## What I measured

The setup was Spring Boot 4.1 on JDK 25 (Temurin 25.0.3, `gcr.io/distroless/java25-debian13`), in a container limited to 2 CPUs and 1 GB. Each round started every variant once, in random order, and I ran ten rounds. I ran it under ZGC, G1, Parallel and Serial, then again with a heavier app built on Postgres, Flyway, Spring Security and 80 JPA entities.

{{< chart "paired-shift" >}}
Most dots sit left of zero, though not all of them. Serial was faster in all 10 rounds and G1 in 9, while the ZGC, Parallel and heavy-app intervals still cross zero. Hover a dot to see its value; the table below has the exact numbers.
{{< /chart >}}

{{< results-table >}}

Pooled over all 50 pairs, startup is **2.0% faster** (95% CI 1.0% to 2.8%), with 39 of the 50 pairs faster.

On the small app that's 20–70 ms. Nobody will notice that on a 2-second startup, but it's a real effect.

Private memory drops by 5–7 MB per JVM on the small app and by about 2 MB on the heavy one, where the difference isn't significant. Metaspace drops by 10 MB in every setup, but most of that is accounting, and the memory section below explains why.

Comparing **medians** of different boots makes the heavy app look 2% *slower*, while pairing each boot with its partner from the same round gives a shift of −0.3%, so the table shows paired shifts.

## Why the gain is so small

The default archive covers about 1.4k JDK classes. By the time a Spring app is ready, it has loaded 15–19k. Most of the startup work is loading your own classes, and the default archive can't help with those.

{{< chart "class-coverage" >}}
The archive serves the same ~1.4k JDK classes no matter what the app does, so its share shrinks as the app grows (9.4% of the small app's classes, 7.7% of the heavy one's). I counted them with `-Xlog:class+load` up to Spring's context refresh.
{{< /chart >}}

Because of that, the saving stays roughly **fixed in milliseconds** on both apps and shrinks as a percentage when the app grows.

Besides classes, the archive holds pre-built Java heap objects, like the module graph and interned strings. ZGC can't load those archived objects in JDK 25. [JEP 516](https://openjdk.org/jeps/516), delivered in JDK 26, lifts that restriction.

## Where the memory goes

Metaspace drops by 10 MB, yet the JVM doesn't use 10 MB less memory. Archived classes live in a memory-mapped region, and the Metaspace pool doesn't count it.

{{< chart "memory-change" >}}
Private memory fell by 5–7 MB per JVM on the small app (2 MB on the heavy one). File-backed memory rose by about 3 MB, which is the mapped archive. Metaspace isn't drawn here. It fell by 10 MB in every setup, but it's part of private memory, and most of its drop moved into the file-backed archive.
{{< /chart >}}

That file-backed part **can** be [shared between JVMs](https://docs.oracle.com/en/java/javase/25/vm/class-data-sharing.html) on the same node through the OS page cache, as long as:

- the pods run from the same image layer (the same JDK build);
- the archive maps at its intended address (if it has to be relocated, its pages get written to and stop being shared);
- only the read-only parts of the archive are shared.

I measured one JVM at a time, so I haven't measured the sharing itself.

## Why the flag still matters

Deleting one flag gives you a few MB of private memory back per JVM. Your image carries four archive files either way (~60 MB), one per memory layout: with and without compressed oops, each with and without [compact object headers](https://openjdk.org/jeps/519). You pay for them on every pull, so you might as well use one.

`-Xshare:off` also blocks [AppCDS](https://openjdk.org/jeps/310), an archive of *your* application's classes that runs on the same mechanism. With the flag set, the JVM silently ignores an AppCDS archive. The app still starts normally, and 0 classes come from the archive instead of about 14.7k. That's where the bigger numbers are, and the next post covers them.

## Why the flag might be there

Often, someone added `-Xshare:off` to silence this warning at startup:

```text
Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

You'll see it when a Java agent appends to the boot classpath, and APM agents are the usual case. If you delete the flag and the warning comes back, that's why. Sharing still works for the JDK's own classes.

## How to check your image

First check that the JVM reports sharing:

```bash
docker run --rm --entrypoint java <image> -version
# last line ends with "sharing" -> CDS is on
```

Then look for `-Xshare:off` in the entrypoint **and** in `JAVA_TOOL_OPTIONS` and `JDK_JAVA_OPTIONS`. The `docker run` above skips your Deployment's environment, so check the running pod too:

```bash
kubectl debug -it <pod> --image=busybox --target=<container> -- grep jsa /proc/1/maps
```

To see which archive got mapped (ZGC uses `classes_nocoops.jsa`):

```bash
java -XX:+UseZGC -Xlog:cds -version | grep Opened
```

In CI, smoke-test with `-Xshare:on`, which [fails loudly](https://docs.oracle.com/en/java/javase/25/docs/specs/man/java.html) when sharing can't be used. Give it **the same GC and heap flags as production**, or you'll be testing a different archive from the one production maps. Never put `-Xshare:on` in the production entrypoint; the `java` reference says it's for testing only.

JDK 11 ships no default archive, but `java -Xshare:dump` creates one.

## Caveats

These are demo apps (about 15k and 19k classes), run on one machine (Docker Desktop on an arm64 Mac) in one session, with ten paired boots per variant. I measured startup and memory, nothing else, so measure your own service.

## Method and data

- Startup is Spring's `process running for`. CPU is utime+stime of PID 1, read about a second after the app reports healthy. Memory comes from `/proc/1/status` and the actuator.
- The paired shift is the [Hodges-Lehmann estimate](https://doi.org/10.1214/aoms/1177704172) over the ten rounds, and its 95% CI is a [bootstrap](https://doi.org/10.1214/aos/1176344552) of that estimate (10,000 resamples, fixed seed). p-values come from exact [Wilcoxon signed-rank tests](https://doi.org/10.2307/3001968) (Serial p = 0.002, G1 p = 0.006). Numbers are rounded half away from zero.
- Class counts come from `-Xlog:class+load` with default CDS and ZGC, up to Spring's context refresh (`measure_coverage.sh`).
- Scripts, raw results and both demo apps live in [`benchmarks/default-cds`](https://github.com/diegourban/diegourban.github.io/tree/master/benchmarks/default-cds), where `make_charts.py` computes every number in the charts and the table from the raw results.
