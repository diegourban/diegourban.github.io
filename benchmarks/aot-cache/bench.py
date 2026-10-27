#!/usr/bin/env python3
"""Startup, warm-up and memory for default CDS, AppCDS and the AOT cache, in rounds.

  python3 bench.py ROUNDS OUT.jsonl     # e.g. 10 results.jsonl; 1 pilot.jsonl for a test round

Every round starts each variant once, in a random order, so each pair of variants can be compared within a round.
Per run: startup (Spring's "process running for"), readiness from the host, memory and CPU one second after
readiness, then WARMUP requests of the common.py workload (latency of each, in µs), then memory and CPU again.
CDS/AOT warning lines from the JVM log are kept, so a cache that failed to load shows up in the data.
Images come from train.py. Appends one JSON line per run, so an interrupted session keeps what it measured.
"""
import json, random, re, sys, time
import common as c

WARMUP = 2000
GCS = {"zgc": "-XX:+UseZGC", "g1": "-XX:+UseG1GC", "parallel": "-XX:+UseParallelGC", "serial": "-XX:+UseSerialGC"}

VARIANTS = {}  # name -> (image, app, flags)
for gc, flag in GCS.items():
    VARIANTS[f"cds_{gc}"] = ("aotbench:small", "small", [flag])
    VARIANTS[f"appcds_{gc}"] = ("aotbench:small", "small", [flag, f"-XX:SharedArchiveFile=/app/caches/appcds-{gc}.jsa"])
    VARIANTS[f"aot_{gc}"] = ("aotbench:small", "small", [flag, f"-XX:AOTCache=/app/caches/aot-{gc}.aot"])
for n in (0, 200, 1000, 20000):
    VARIANTS[f"aot_g1_n{n}"] = ("aotbench:small", "small", [GCS["g1"], f"-XX:AOTCache=/app/caches/aot-g1-n{n}.aot"])
VARIANTS["heavy_cds_zgc"] = ("aotbench:heavy", "heavy", [GCS["zgc"]])
VARIANTS["heavy_appcds_zgc"] = ("aotbench:heavy", "heavy", [GCS["zgc"], "-XX:SharedArchiveFile=/app/caches/appcds-zgc.jsa"])
VARIANTS["heavy_aot_zgc"] = ("aotbench:heavy", "heavy", [GCS["zgc"], "-XX:AOTCache=/app/caches/aot-zgc.aot"])
for jdk in ("25", "26"):
    VARIANTS[f"t{jdk}_cds_zgc"] = (f"aotbench:t{jdk}", "small", [GCS["zgc"]])
    VARIANTS[f"t{jdk}_aot_zgc"] = (f"aotbench:t{jdk}", "small", [GCS["zgc"], "-XX:AOTCache=/app/caches/aot-zgc.aot"])
    # Added after the review, measured in a second session (results_jdk.jsonl) with the four variants above.
    VARIANTS[f"t{jdk}_appcds_zgc"] = (f"aotbench:t{jdk}", "small", [GCS["zgc"], "-XX:SharedArchiveFile=/app/caches/appcds-zgc.jsa"])

STARTED = re.compile(r"Started \w+ in ([\d.]+) seconds \(process running for ([\d.]+)\)")


def sample(cname, port):
    """Memory (MB) and CPU seconds of the JVM (PID 1 in the container), plus Metaspace and loaded classes."""
    raw = c.sh("docker", "run", "--rm", f"--pid=container:{cname}", "busybox:1.37.0", "sh", "-c",
               "cat /proc/1/status; echo STAT; cat /proc/1/stat")
    status, stat = raw.split("STAT")
    kb = {k: int(v.split()[0]) for k, v in re.findall(r"^(VmRSS|RssAnon|RssFile|RssShmem):\s+(.+)$", status, re.M)}
    fields = stat.split(")")[-1].split()
    metric = lambda name: (c.get_json(port, f"/actuator/metrics/{name}") or {}).get("measurements", [{}])[0].get("value")
    meta = (c.get_json(port, "/actuator/metrics/jvm.memory.used?tag=id:Metaspace") or {}).get("measurements", [{}])[0].get("value")
    return {"rss": kb.get("VmRSS", 0) / 1024, "rss_anon": kb.get("RssAnon", 0) / 1024, "rss_file": kb.get("RssFile", 0) / 1024,
            "rss_shmem": kb.get("RssShmem", 0) / 1024, "cpu_s": (int(fields[11]) + int(fields[12])) / 100,
            "metaspace": (meta or 0) / 2**20, "classes": metric("jvm.classes.loaded")}


def run(name):
    image, app, flags = VARIANTS[name]
    jar = c.SMALL_JAR if app == "small" else c.HEAVY_JAR
    net = ["--network", c.NETWORK] if app == "heavy" else []
    if app == "heavy":
        c.postgres_reset()
    for attempt in range(3):  # docker exits 125 when it can't create the container, e.g. the random port is taken
        port = random.randint(20000, 40000)
        cname = f"aotbench-{name}-{port}"
        t0 = time.monotonic()
        p = c.subprocess.run(["docker", "run", "-d", "--name", cname, *c.LIMITS, *net, "-p", f"{port}:8080",
                              "--entrypoint", "java", image, *flags, "-jar", jar], capture_output=True, text=True)
        if p.returncode == 0:
            break
        c.sh("docker", "rm", "-f", cname, check=False)
    else:
        raise RuntimeError(f"docker run failed 3 times: {p.stderr.strip()[:300]}")
    try:
        if c.wait_ready(port, 180) is None:
            raise RuntimeError(f"{name}: not ready\n" + c.sh("docker", "logs", cname)[-2000:])
        ready_wall = time.monotonic() - t0  # from docker run, like the post-2 benchmark
        time.sleep(1)
        after_ready = sample(cname, port)
        lat = c.drive(port, c.requests(app, WARMUP))
        after_warmup = sample(cname, port)
        logs = c.sh("docker", "logs", cname)
    finally:
        c.sh("docker", "rm", "-f", cname, check=False)
    m = STARTED.search(logs)
    return {"variant": name, "started": float(m.group(1)), "process": float(m.group(2)), "ready_wall": round(ready_wall, 3),
            "after_ready": after_ready, "after_warmup": after_warmup, "latency_us": lat,
            "jvm_warnings": [l for l in logs.splitlines() if re.search(r"\[(warning|error)\s*\]\[(aot|cds)", l)][:5]}


def main():
    rounds = int(sys.argv[1]) if len(sys.argv) > 1 else 1
    out = sys.argv[2] if len(sys.argv) > 2 else "pilot.jsonl"
    only = sys.argv[3].split(",") if len(sys.argv) > 3 else list(VARIANTS)
    c.postgres_up()
    with open(out, "a") as f:
        for rnd in range(rounds):
            order = only[:]
            random.shuffle(order)
            for name in order:
                try:
                    rec = {"round": rnd, **run(name)}
                except Exception as e:  # a variant that fails to start is a result too; keep the session going
                    rec = {"round": rnd, "variant": name, "failed": str(e)[:1500]}
                    f.write(json.dumps(rec) + "\n")
                    f.flush()
                    print(f"r{rnd} {name:18} FAILED {str(e).splitlines()[0][:100]}", flush=True)
                    continue
                f.write(json.dumps(rec) + "\n")
                f.flush()
                lat = rec["latency_us"]
                print(f"r{rnd} {name:18} process {rec['process']:.2f}s ready {rec['ready_wall']:.2f}s "
                      f"first {lat[0]/1000:.1f}ms med1-100 {sorted(lat[:100])[50]/1000:.2f}ms "
                      f"rss {rec['after_ready']['rss']:.0f}->{rec['after_warmup']['rss']:.0f}MB"
                      + (f" WARN {rec['jvm_warnings'][0][:80]}" if rec["jvm_warnings"] else ""), flush=True)


if __name__ == "__main__":
    main()
