#!/usr/bin/env python3
"""The AOT cache and AppCDS with the OpenTelemetry Java agent, reusing the aot-cache harness.

  python3 agents.py train            # AppCDS archive trained WITH the agent (G1), baked into aotbench:small
  python3 agents.py check            # single runs: what each cache loads with the agent attached -> checks.json
  python3 agents.py bench ROUNDS     # startup and warm-up rounds -> results.jsonl

The agent is OpenTelemetry's Java agent 2.23.0, already in the image at /app/otel.jar, with every default
instrumentation on and the exporters off, so no backend is needed. Training uses post 3's 5,000-request workload.
An AOT cache can't be trained with the agent on aarch64: every attempt crashed the JVM on JDK 25.0.3, 25.0.4 and
26.0.2 (probe_crash.sh -> probe_crash.txt), so only AppCDS is trained here.
"""
import json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
AOT = os.path.join(HERE, "..", "aot-cache")
sys.path.insert(0, AOT)
import bench, common as c, measure_oneoffs as oneoffs, train  # noqa: E402

G1 = "-XX:+UseG1GC"
AGENT = ["-javaagent:/app/otel.jar", "-Dotel.traces.exporter=none", "-Dotel.metrics.exporter=none", "-Dotel.logs.exporter=none"]

# An AppCDS dump refuses to run with an agent ("Must enable AllowArchivingWithJavaAgent in order to run Java agent
# during CDS dumping"); the flag is diagnostic. do_train records that refusal before training with the flag.
ALLOW_AGENT = ["-XX:+UnlockDiagnosticVMOptions", "-XX:+AllowArchivingWithJavaAgent"]
TRAININGS = [
    ("small", "small", "cdsdemo:base", "appcds-g1-otel.jsa", [G1, *AGENT, *ALLOW_AGENT, "-XX:ArchiveClassesAtExit=/out/appcds-g1-otel.jsa"], train.MAIN_REQUESTS),
]

VARIANTS = {
    # Without the agent: post 3's setups, re-run in the same rounds.
    "cds_g1": ("aotbench:small", "small", [G1]),
    "appcds_g1": ("aotbench:small", "small", [G1, "-XX:SharedArchiveFile=/app/caches/appcds-g1.jsa"]),
    "aot_g1": ("aotbench:small", "small", [G1, "-XX:AOTCache=/app/caches/aot-g1.aot"]),
    # With the agent.
    "otel_cds_g1": ("aotbench:small", "small", [G1, *AGENT]),
    # The archive records AllowArchivingWithJavaAgent; without the same flag at runtime it's rejected
    # ("The setting of the AllowArchivingWithJavaAgent is different from the setting in the shared archive file").
    "otel_appcds_g1": ("aotbench:small", "small", [G1, *AGENT, *ALLOW_AGENT, "-XX:SharedArchiveFile=/app/caches/appcds-g1-otel.jsa"]),
    "otel_appcds_noflag_g1": ("aotbench:small", "small", [G1, *AGENT, "-XX:SharedArchiveFile=/app/caches/appcds-g1-otel.jsa"]),
    # Archives trained without the agent, started with it (post 3's trap, and its AppCDS counterpart).
    "otel_appcds_untrained_g1": ("aotbench:small", "small", [G1, *AGENT, "-XX:SharedArchiveFile=/app/caches/appcds-g1.jsa"]),
    "otel_aot_untrained_g1": ("aotbench:small", "small", [G1, *AGENT, "-XX:AOTCache=/app/caches/aot-g1.aot"]),
}


def refused_dumps():
    """Training runs with the agent and no AllowArchivingWithJavaAgent, up to the context refresh: what the JVM says."""
    out = {}
    for name, flag in (("appcds", "-XX:ArchiveClassesAtExit=/tmp/x.jsa"), ("aot", "-XX:AOTCacheOutput=/tmp/x.aot")):
        p = c.subprocess.run(["docker", "run", "--rm", *c.LIMITS, "--entrypoint", "java", "cdsdemo:base", G1, *AGENT, flag,
                              "-Dspring.context.exit=onRefresh", "-jar", c.SMALL_JAR], capture_output=True, text=True)
        lines = [l.strip()[:200] for l in (p.stdout + p.stderr).splitlines()
                 if any(k in l for k in ("AllowArchivingWithJavaAgent", "Error occurred", "AOTCache creation", "[error]", "[warning][aot", "[warning][cds"))]
        out[name] = {"exit_code": p.returncode, "lines": lines[:5]}
        print(name, json.dumps(out[name])[:300], flush=True)
    return out


def do_train():
    json.dump(refused_dumps(), open(os.path.join(HERE, "training_without_allow_flag.json"), "w"), indent=1)
    records = [train.train(*t) for t in TRAININGS]
    json.dump(records, open(os.path.join(HERE, "training.json"), "w"), indent=1)
    train.bake_images({"small"})


def do_check():
    out = {name: oneoffs.aot_log(image, flags) for name, (image, _, flags) in VARIANTS.items()}
    for k, v in out.items():
        print(f"{k:34} exit {v['exit_code']} cache {v['classes_from_cache']}/{v['classes_loaded']} {v['lines'][:2]}", flush=True)
    json.dump(out, open(os.path.join(HERE, "checks.json"), "w"), indent=1)


def do_bench(rounds):
    bench.VARIANTS.clear()
    bench.VARIANTS.update(VARIANTS)
    sys.argv = ["bench.py", str(rounds), os.path.join(HERE, "results.jsonl")]
    bench.main()


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else ""
    if cmd == "train":
        c.postgres_up()
        do_train()
    elif cmd == "check":
        do_check()
    elif cmd == "bench":
        do_bench(int(sys.argv[2]))
    else:
        sys.exit(__doc__)
