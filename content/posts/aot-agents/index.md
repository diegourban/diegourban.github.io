---
title: "The OpenTelemetry agent and JDK 25's AOT cache don't mix"
date: 2026-11-04
draft: false
description: "On JDK 25, training an AOT cache with the OpenTelemetry Java agent attached never produced a cache. The agent added 1.6 s to a Spring Boot app's startup, and AppCDS won back only about 0.15 s, for 70 to 80 MB more memory."
tags: ["java", "jvm", "aot", "appcds", "opentelemetry", "spring-boot", "startup"]
ShowToc: true
cover:
  image: cover.png
  relative: true
  alt: "The JVM refusing to write an AOT cache with the OpenTelemetry agent attached: Critical class java.lang.Class has been excluded. Without the agent the AOT cache saves 1.0 s of startup; with it, 0 of 15 training runs wrote a cache and 13 crashed the JVM"
  hiddenInSingle: true
  hiddenInList: true
ai_disclosure: ai-generated
references:
  - author: "OpenJDK"
    title: "8356693: AOT assembly phase fails with -javaagent (pull request #25170)"
    venue: "openjdk/jdk, GitHub"
    year: 2025
    url: "https://github.com/openjdk/jdk/pull/25170"
  - author: "OpenJDK"
    title: "JEP 483: Ahead-of-Time Class Loading & Linking"
    venue: "Delivered in JDK 24"
    year: 2025
    url: "https://openjdk.org/jeps/483"
  - author: "OpenTelemetry"
    title: "Java Agent"
    venue: "OpenTelemetry documentation"
    url: "https://opentelemetry.io/docs/zero-code/java/agent/"
  - author: "OpenTelemetry"
    title: "Spring Boot starter"
    venue: "OpenTelemetry documentation"
    url: "https://opentelemetry.io/docs/zero-code/java/spring-boot-starter/"
  - author: "OpenTelemetry"
    title: "opentelemetry-java-instrumentation, release 2.23.0"
    venue: "GitHub"
    url: "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.23.0"
  - author: "Amazon Corretto"
    title: "AOT cache assembly crashes when training ran with a -javaagent (issue #84)"
    venue: "corretto-25, GitHub"
    year: 2026
    url: "https://github.com/corretto/corretto-25/issues/84"
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

[My AOT cache post](/posts/aot-cache/) warned that an agent added only at runtime makes the JVM throw the AOT cache away, and suggested training the cache with the agent attached. With the OpenTelemetry Java agent, that doesn't work. None of 15 training runs on JDK 25 and 26 wrote a cache, and 13 of them crashed the JVM.

The agent also costs more startup than any archive won back. It added 1.6 seconds to my Spring Boot app's startup on JDK 25, while the AOT cache saves about 1 second without it. AppCDS did load with the agent attached, and an AppCDS archive trained without the agent won back about 0.15 seconds, for 70 to 80 MB more memory.

## What I measured

The app is the same small Spring Boot 4.1 service as in that post, on JDK 25 (Temurin 25.0.3) with G1, in containers limited to 2 CPUs and 1 GB. I attached [OpenTelemetry's Java agent](https://opentelemetry.io/docs/zero-code/java/agent/) [2.23.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.23.0) with its default instrumentation and the exporters switched off, so nothing needs a backend. Archives were trained on the same 5,000 requests as there. All eight setups ran in each of ten rounds, in random order.

{{< chart "paired-shift" "startup" >}}
The baseline is a single setup: no agent, and the JDK's default archive. Each setup with the agent ends up to the right of every setup without it.
{{< /chart >}}

{{< results-table >}}

The agent loads about 3,800 more classes (19,132 against 15,317), and the median startup went from 1.92 s to 3.45 s. Warm-up got slower as well, by 0.57 s over the first 2,000 requests after startup.

## No AOT cache with the agent's defaults

I tried 15 times, five each on Temurin 25.0.3, 25.0.4.1 and 26.0.2.1, and none wrote a cache. Each run trained only until Spring's context refresh, to keep it short. Thirteen of them crashed the JVM with a `SIGSEGV`. On JDK 25 the crash was in the code that decides which classes go into the cache (`SystemDictionaryShared::check_for_exclusion`), on JDK 26 in the code that prepares the training data. The other two ended without a crash report and without a cache.

According to the training log, the agent rewrites core JDK classes, and the cache leaves out every class that was rewritten, with lines like `Skipping java/lang/Class: From ClassFileLoadHook`. Among the skipped ones are `java.lang.ClassLoader` and `jdk.internal.loader.BuiltinClassLoader`, which the JVM needs to build any cache. On JDK 26, with AOT class linking switched off (`-XX:-AOTClassLinking`) to get past the crash, the JVM names the problem:

```text
Critical class java.lang.Class has been excluded. AOT configuration file cannot be written.
```

[JEP 483](https://openjdk.org/jeps/483) originally ruled out agents that rewrite classes. JDK 25 relaxed that ([JDK-8356693](https://github.com/openjdk/jdk/pull/25170)): an agent may run during training, and the cache leaves out every class it transformed, which is where the `Skipping` lines come from. That can work for an agent that only touches application classes. The OpenTelemetry agent also rewrites `java.lang.Class` and the JDK's class loaders, and the JVM can't build a cache without those. A [Corretto issue](https://github.com/corretto/corretto-25/issues/84) reports a similar crash with a different agent, but its fix is already in JDK 26, which failed here as well.

I also tried switching off the agent's instrumentations that rewrite JDK classes, on JDK 25.0.3. With `internal-reflection` and `internal-class-loader` off, training still crashed. With `executors` off as well, it wrote a 59 MB cache, but the app didn't start with it. It didn't start without a cache either: with those instrumentations off, the main thread died with a `NoClassDefFoundError` for one of the agent's own classes. So I found no setting where both the agent and a cache work. I didn't try newer OpenTelemetry releases or JDK 27 early-access builds, and found no open issue about this in the OpenTelemetry or OpenJDK trackers.

## AppCDS loads, but only partly

AppCDS didn't get that change, and its way around the restriction is a diagnostic flag. A training run with the agent attached stops with `Must enable AllowArchivingWithJavaAgent in order to run Java agent during CDS dumping`. With `-XX:+UnlockDiagnosticVMOptions -XX:+AllowArchivingWithJavaAgent` the archive gets written, but every start that uses it then needs the same two flags, and JDK 25 warns that such an archive "should be used for testing purposes only and should not be used in a production environment". JDK 26 dropped the flag (`Unrecognized VM option`) and refuses to dump an archive with any agent attached, which rules it out for me.

An archive trained without the agent needs no flags and loads fine with the agent attached. Compared with the agent on the default archive, it started 142 ms faster (95% CI 106 to 222 ms, faster in nine of ten rounds) and took 219 ms off the 2,000 requests (faster in all ten). The flagged archive was 229 ms faster at startup, but head to head the two archives weren't measurably different (82 ms apart, CI from 143 ms faster to 33 ms slower).

Both archives served only a fraction of the classes: 5,111 of 18,918 for the one trained with the agent, and 2,573 of 18,941 for the one trained without it. Without the agent, AppCDS served 14,693 of 15,139. Serving twice as many classes didn't make the first archive measurably faster than the second. At startup the JVM prints the reason, the same warning [the first post](/posts/default-cds/#why-the-flag-might-be-there) ran into with APM agents:

```text
Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

The agent puts its own classes on the boot class path, and from then on the JVM only shares classes loaded by the boot loader. Spring's classes and the application's own come from the application class loader, so they're loaded from the JARs again on every start. Of the 13,807 classes the agent-trained archive didn't serve, only 198 were left out because the agent had rewritten them.

Memory went the other way from what I expected. Without the agent, AppCDS moved about 15 MB from private memory into the mapped archive. Compared with the agent on the default archive, which is mapped too, both archives added 50 to 60 MB of private memory and about 18 MB of mapped file, so resident memory rose by 70 to 80 MB. I haven't found the reason.

The setup the AOT cache post warned about, an AOT cache trained without the agent, started 86 ms faster than the agent alone. Its 95% CI runs from 196 ms faster to 11 ms slower, and ten rounds could only have caught a difference of about 140 ms, so I count it as nothing measurable. According to its log, the JVM drops the whole cache, because the agent adds the `java.instrument` module and the cache was built without it.

## What I'd do

Before tuning anything, measure what the agent costs your app's startup. Here it was 1.6 s. OpenTelemetry's own documentation suggests its [Spring Boot starter](https://opentelemetry.io/docs/zero-code/java/spring-boot-starter/) when the agent's startup overhead exceeds your requirements. I haven't measured the starter yet.

If you keep the agent, train AppCDS without it. It needs no diagnostic flags, and you can weigh the startup it saves against the memory it costs on your own app. Check with `-Xlog:class+load` that classes really come from the archive. While the agent is attached, I'd keep the AOT cache and `AllowArchivingWithJavaAgent` out of production.

## Caveats

One demo app on one machine (Docker Desktop on an arm64 Mac, so no x86-64), G1 only, ten rounds per setup and one archive per setup. I tested a single agent, OpenTelemetry 2.23.0. The benchmark used its defaults, and only the AOT cache attempt above tried it with instrumentations switched off. I didn't try other agents.

## Method and data

- Startup is Spring's `process running for`, compared within each round. The shift is the [Hodges-Lehmann estimate](https://doi.org/10.1214/aoms/1177704172) of the paired differences, its 95% interval a [bootstrap](https://doi.org/10.1214/aos/1176344552) of that estimate (10,000 resamples, fixed seed), and p comes from exact [Wilcoxon signed-rank tests](https://doi.org/10.2307/3001968), with the same code as the AOT cache post. Memory is sampled from `/proc/1/status` after readiness and after the requests.
- Classes from the archive are counted with `-Xlog:class+load` in single runs (`checks.json`).
- Everything is in [`benchmarks/aot-agents`](https://github.com/diegourban/diegourban.github.io/tree/master/benchmarks/aot-agents): `agents.py` trains, checks and runs the rounds on the AOT cache post's harness (`results.jsonl`), `probe_crash.sh` and `probe_repeat.sh` record the crashes, `measure_noint.sh` the attempt with instrumentations switched off (`noint_evidence.txt`), `measure_review.sh` records the JVM messages quoted here (`review_evidence.txt`), and `analyze.py` and `make_charts.py` compute every number.
