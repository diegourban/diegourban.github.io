---
title: "Is anything switching off your JVM's free startup optimisation?"
date: 2026-10-18
draft: false
description: "Removing a leftover -Xshare:off: what the default CDS archive does and doesn't buy a Spring Boot 4 app on JDK 25, measured across four GCs."
tags: ["java", "jvm", "cds", "spring-boot", "performance", "startup"]
ShowToc: true
---

Your Java image already contains a startup optimisation. Is anything switching it off?

Every JDK since 12 ships **CDS (Class Data Sharing)** archives: the JDK's core classes stored already parsed and laid out, in `lib/server/*.jsa`. At startup the JVM memory-maps one of them instead of loading those classes from scratch. It's on by default, and there's nothing to configure.

Unless your JVM flags say `-Xshare:off`. Ours did, in several services. It was a leftover from an old base image, and nobody had written down why.

I expected deleting it to make startup noticeably faster. It didn't, and I think that's the more useful finding: **default CDS saves a fixed few tens of milliseconds of JVM startup, while your startup time is mostly your own classes.**

## What I measured

Spring Boot 4.1 on JDK 25 (Temurin 25.0.3, `gcr.io/distroless/java25-debian13`), in a container limited to 2 CPUs and 1 GB. Each round started every variant once, in random order, ten rounds in total. I ran it under ZGC, G1, Parallel and Serial, and again with a heavier app: Postgres, Flyway, Spring Security and 80 JPA entities.

| Setup | Startup, flag on → removed (median) | Paired shift (95% CI) | Faster with CDS | Private memory (RssAnon) | Metaspace |
|---|---|---|---|---|---|
| ZGC | 1.99 → 1.94 s | −20 ms (−65 to +51) | 7/10 | 173 → 166 MB | 82.7 → 72.3 MB |
| G1 | 1.89 → 1.87 s | −40 ms (−109 to −18) | 9/10 | 214 → 209 MB | 82.3 → 72.4 MB |
| Parallel | 1.84 → 1.79 s | −27 ms (−68 to +32) | 7/10 | 258 → 251 MB | 82.5 → 72.5 MB |
| Serial | 1.88 → 1.81 s | −69 ms (−104 to −49) | 10/10 | 204 → 199 MB | 82.6 → 72.5 MB |
| Heavy app (ZGC) | 4.43 → 4.50 s | −11 ms (−145 to +162) | 6/10 | 218 → 216 MB | 104.0 → 94.2 MB |

Pooled over all 50 pairs, startup is **2.1% faster** (95% CI 1.5% to 3.2%), and 39 of the 50 pairs were faster.

So:

- **Startup time: a small but real gain, about 2%, or 20–70 ms.** It's too small to notice on a 2-second startup, but it isn't noise.
- **Private memory: 5–7 MB less per JVM** on the small app, and about 2 MB on the heavy one (where the difference isn't significant).
- **Metaspace: 10 MB less in every setup.** Most of that is accounting, explained below.

Comparing **medians** of different boots, the heavy app looks 2% *slower*. Comparing each boot with its partner from the same round, the shift is −0.2%. The table uses paired shifts for that reason.

## Why so little time?

The default archive covers about 1.5k JDK classes. A Spring app loads 16–19k. Most of the work is loading your own classes, and the default archive can't help with those.

That makes the saving roughly **fixed in milliseconds**: about the same on the small app and on the heavy one. As a percentage it shrinks as the app grows.

The archive also stores pre-built Java heap objects (the module graph, interned strings), not just classes. In JDK 25 ZGC can't load those archived objects; JEP 516, planned for JDK 26, is meant to fix that.

## Where the memory goes

Metaspace drops by 10 MB, but the JVM doesn't use 10 MB less memory. Archived classes live in a memory-mapped region that isn't counted in the Metaspace pool. In the measurements:

- **Private (anonymous) memory** went down by 5–7 MB.
- **File-backed memory** went up by 3 MB: that's the mapped archive.

The file-backed part **can** be shared between JVMs on the same node, through the OS page cache, as long as:

- the pods run from the same image layer (the same JDK build);
- the archive maps at its intended address (if it has to be relocated, its pages get written to and stop being shared);
- only the read-only parts of the archive are shared.

I measured one JVM at a time, so I haven't measured the sharing itself.

## Why the flag still matters

1. **Memory.** A few MB of private memory back per JVM, at the cost of deleting one flag.
2. **The files ship anyway.** The image carries four archives (~60 MB), one per memory layout: with and without compressed oops, each with and without compact object headers. You pay for them in every pull, so you may as well use one.
3. **It blocks the next step.** AppCDS, an archive of *your* application's classes, builds on the same mechanism. With `-Xshare:off`, an AppCDS archive is silently ignored: the app starts normally, no warning is printed, and 0 classes come from the archive instead of about 14.7k. That's where the bigger numbers are, and it's the next post.

## Why the flag might be there

A common reason for `-Xshare:off` is silencing this warning at startup:

```text
Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

It appears when a Java agent (APM agents are the usual case) appends to the boot classpath. If you delete the flag and the warning comes back, that's why. Sharing still works for the JDK's own classes.

## How to check your image

The JVM should report sharing:

```bash
docker run --rm --entrypoint java <image> -version
# last line ends with "sharing" -> CDS is on
```

Look for `-Xshare:off` in the entrypoint **and** in `JAVA_TOOL_OPTIONS` and `JDK_JAVA_OPTIONS`. The `docker run` above skips your Deployment's environment, so also check the running pod:

```bash
kubectl debug -it <pod> --image=busybox --target=<container> -- grep jsa /proc/1/maps
```

To see which archive was mapped (ZGC uses `classes_nocoops.jsa`):

```bash
java -XX:+UseZGC -Xlog:cds -version | grep Opened
```

In CI, smoke-test with `-Xshare:on`, which fails loudly if sharing can't be used. Pass it **the same GC and heap flags as production**. Otherwise you test a different archive from the one production maps. Never put `-Xshare:on` in the production entrypoint.

On **JDK 11** there is no default archive in the JDK; `java -Xshare:dump` creates one.

## Caveats

These are demo apps (about 16k and 19k classes), on one machine (Docker Desktop on an arm64 Mac), in one session, ten paired boots per variant. I measured startup and memory only. Measure your own service.

## Method and data

- **Startup** is Spring's `process running for`. **CPU** is utime+stime of PID 1, read about a second after the app reports healthy. **Memory** comes from `/proc/1/status` and the actuator.
- **Paired shift** is the Hodges-Lehmann estimate over the ten rounds. The 95% CI is a bootstrap of the median paired difference. p-values come from exact Wilcoxon signed-rank tests (Serial p = 0.002, G1 p = 0.006).
- **Scripts, raw results and both demo apps:** [`benchmarks/default-cds`](https://github.com/diegourban/diegourban.github.io/tree/master/benchmarks/default-cds). `paired_stats.py` reproduces every number in the table.

---

The fix is deleting one flag. It's no speed miracle: a few tens of milliseconds and a few MB of private memory per JVM. But it puts the files you already ship to work, and it opens the way for real startup gains.

*Have you found `-Xshare:off`, or another flag nobody can explain, in your JVM options?*
