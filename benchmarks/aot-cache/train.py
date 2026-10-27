#!/usr/bin/env python3
"""Trains every AppCDS archive and AOT cache the benchmark needs, then bakes them into the benchmark images.

  python3 train.py            # all trainings, then the images
  python3 train.py signal     # only the logged 20,000-request run for the "trained enough" signal
  python3 train.py only t25,t26   # retrain some image sets, keeping the other records

Each training run starts the app with the runtime GC flag, sends the workload from common.py, then asks the app to
shut down over HTTP so the JVM exits cleanly and writes the archive or cache (requests=0 trains up to Spring's
context refresh instead). Archives land in .caches/ (git-ignored, ~100 MB each); training.json records time and size.
Images: aotbench:small (cdsdemo:base + caches), aotbench:heavy (heavydemo:base + caches),
aotbench:t25 / aotbench:t26 (full Temurin JDK 25 / 26 + caches, see Dockerfile.temurin) for the ZGC side-run.
"""
import json, os, random, subprocess, sys, time
import common as c

HERE = os.path.dirname(os.path.abspath(__file__))
CACHES = os.path.join(HERE, ".caches")
MAIN_REQUESTS = 5000
GCS = {"zgc": "-XX:+UseZGC", "g1": "-XX:+UseG1GC", "parallel": "-XX:+UseParallelGC", "serial": "-XX:+UseSerialGC"}

# (image set, app, base image, archive file name, flags, requests)
TRAININGS = []
for gc, flag in GCS.items():
    TRAININGS.append(("small", "small", "cdsdemo:base", f"appcds-{gc}.jsa", [flag, "-XX:ArchiveClassesAtExit=/out/appcds-{gc}.jsa".format(gc=gc)], MAIN_REQUESTS))
    TRAININGS.append(("small", "small", "cdsdemo:base", f"aot-{gc}.aot", [flag, f"-XX:AOTCacheOutput=/out/aot-{gc}.aot"], MAIN_REQUESTS))
for n in (0, 200, 1000, 20000):  # the G1 training-length curve; 5,000 is aot-g1.aot above
    TRAININGS.append(("small", "small", "cdsdemo:base", f"aot-g1-n{n}.aot", [GCS["g1"], f"-XX:AOTCacheOutput=/out/aot-g1-n{n}.aot"], n))
TRAININGS.append(("heavy", "heavy", "heavydemo:base", "appcds-zgc.jsa", [GCS["zgc"], "-XX:ArchiveClassesAtExit=/out/appcds-zgc.jsa"], MAIN_REQUESTS))
TRAININGS.append(("heavy", "heavy", "heavydemo:base", "aot-zgc.aot", [GCS["zgc"], "-XX:AOTCacheOutput=/out/aot-zgc.aot"], MAIN_REQUESTS))
for jdk in ("25", "26"):
    TRAININGS.append((f"t{jdk}", "small", f"aotbench:t{jdk}-base", "aot-zgc.aot", [GCS["zgc"], "-XX:AOTCacheOutput=/out/aot-zgc.aot"], MAIN_REQUESTS))
    # Added after the review: AppCDS on the same images, so the JDK 25 vs 26 ZGC comparison has an AppCDS baseline.
    TRAININGS.append((f"t{jdk}", "small", f"aotbench:t{jdk}-base", "appcds-zgc.jsa", [GCS["zgc"], "-XX:ArchiveClassesAtExit=/out/appcds-zgc.jsa"], MAIN_REQUESTS))


def train(image_set, app, image, archive, flags, n, extra=(), keep_logs=None):
    out = os.path.join(CACHES, image_set)
    os.makedirs(out, exist_ok=True)
    target = os.path.join(out, archive)
    if os.path.exists(target):
        os.remove(target)
    port = random.randint(20000, 40000)
    name = f"aot-train-{port}"
    jar = c.SMALL_JAR if app == "small" else c.HEAVY_JAR
    net = ["--network", c.NETWORK] if app == "heavy" else []
    if app == "heavy":
        c.postgres_reset()
    stop = ["-Dspring.context.exit=onRefresh"] if n == 0 else c.SHUTDOWN_PROPS
    t0 = time.monotonic()
    c.sh("docker", "run", "-d", "--name", name, *c.LIMITS, *net, "-p", f"{port}:8080", "-v", f"{out}:/out",
         "--entrypoint", "java", image, *flags, *extra, *stop, "-jar", jar)
    uptime_at_start = None
    if n > 0:
        if c.wait_ready(port, 180) is None:
            raise RuntimeError(f"{archive}: not ready\n" + c.sh("docker", "logs", name)[-2000:])
        uptime_at_start = (c.get_json(port, "/actuator/metrics/process.uptime") or {}).get("measurements", [{}])[0].get("value")
        c.drive(port, c.requests(app, n))
        conn = __import__("http.client").client.HTTPConnection("localhost", port, timeout=10)
        conn.request("POST", "/actuator/shutdown")
        conn.getresponse().read()
    code = int(c.sh("docker", "wait", name).strip())
    seconds = time.monotonic() - t0
    logs = c.sh("docker", "logs", name)
    c.sh("docker", "rm", "-f", name)
    if code != 0 or not os.path.exists(target):
        raise RuntimeError(f"{archive}: exit {code}, no archive\n{logs[-2000:]}")
    rec = {"image_set": image_set, "app": app, "base_image": image, "archive": archive, "flags": flags, "requests": n,
           "train_seconds": round(seconds, 1), "bytes": os.path.getsize(target), "uptime_at_workload_start": uptime_at_start}
    print(json.dumps(rec), flush=True)
    return rec


def migrate_heavy():
    """One uncounted boot so Flyway creates the 80 tables before any heavy-app training or measurement."""
    c.sh("docker", "run", "--rm", *c.LIMITS, "--network", c.NETWORK, "--entrypoint", "java", "heavydemo:base",
         "-Dspring.context.exit=onRefresh", "-jar", c.HEAVY_JAR)


def build_images():
    for jdk in ("25", "26"):
        c.sh("docker", "build", "-q", "--build-arg", f"JDK={jdk}", "-t", f"aotbench:t{jdk}-base", "-f",
             os.path.join(HERE, "Dockerfile.temurin"), HERE)


def bake_images(sets=None):
    bases = {"small": "cdsdemo:base", "heavy": "heavydemo:base", "t25": "aotbench:t25-base", "t26": "aotbench:t26-base"}
    for image_set, base in bases.items():
        if sets and image_set not in sets:
            continue
        ctx = os.path.join(CACHES, image_set)
        with open(os.path.join(ctx, "Dockerfile"), "w") as f:
            f.write(f"FROM {base}\nCOPY *.jsa *.aot /app/caches/\n" if any(x.endswith(".jsa") for x in os.listdir(ctx))
                    else f"FROM {base}\nCOPY *.aot /app/caches/\n")
        c.sh("docker", "build", "-q", "-t", f"aotbench:{image_set}", ctx)
        print("built", f"aotbench:{image_set}", flush=True)


def signal_run():
    """A logged 20,000-request G1 training: class loads and JIT compilations over time, plus request timestamps."""
    out = os.path.join(CACHES, "signal")
    os.makedirs(out, exist_ok=True)
    for f in os.listdir(out):
        os.remove(os.path.join(out, f))
    port = random.randint(20000, 40000)
    name = f"aot-signal-{port}"
    c.sh("docker", "run", "-d", "--name", name, *c.LIMITS, "-p", f"{port}:8080", "-v", f"{out}:/out",
         "--entrypoint", "java", "cdsdemo:base", GCS["g1"], "-XX:AOTCacheOutput=/out/signal.aot",
         "-Xlog:class+load=info:file=/out/classload-%p.log:uptime", "-Xlog:jit+compilation=debug:file=/out/jit-%p.log:uptime",
         *c.SHUTDOWN_PROPS, "-jar", c.SMALL_JAR)
    c.wait_ready(port, 120)
    up = c.get_json(port, "/actuator/metrics/process.uptime")["measurements"][0]["value"]
    host0 = time.monotonic()
    marks = []  # (jvm uptime in seconds, requests done)
    reqs = c.requests("small", 20000)
    for i in range(0, len(reqs), 100):
        c.drive(port, reqs[i:i + 100])
        marks.append((round(up + time.monotonic() - host0, 3), i + 100))
    conn = __import__("http.client").client.HTTPConnection("localhost", port, timeout=10)
    conn.request("POST", "/actuator/shutdown")
    conn.getresponse().read()
    c.sh("docker", "wait", name)
    c.sh("docker", "rm", "-f", name)
    events = {"classes": [], "compilations": []}
    for kind, prefix in (("classes", "classload-1.log"), ("compilations", "jit-1.log")):
        for line in open(os.path.join(out, prefix), errors="replace"):
            if line.startswith("[") and "made not entrant" not in line:
                events[kind].append(float(line[1:line.index("s]")]))
    json.dump({"workload_start_uptime": up, "marks": marks, **events}, open(os.path.join(HERE, "signal.json"), "w"))
    print("signal.json:", len(events["classes"]), "class loads,", len(events["compilations"]), "compilations,", len(marks), "marks")


if __name__ == "__main__":
    c.postgres_up()
    if sys.argv[1:] == ["signal"]:
        signal_run()
        sys.exit()
    if sys.argv[1:2] == ["archives"]:  # train named archives only, e.g. "archives t25/appcds-zgc.jsa,t26/appcds-zgc.jsa"
        wanted = {tuple(x.split("/")) for x in sys.argv[2].split(",")}
        path = os.path.join(HERE, "training.json")
        records = [r for r in json.load(open(path)) if (r["image_set"], r["archive"]) not in wanted]
        records += [train(*t) for t in TRAININGS if (t[0], t[3]) in wanted]
        json.dump(records, open(path, "w"), indent=1)
        bake_images({s for s, _ in wanted})
        sys.exit()
    if sys.argv[1:2] == ["only"]:  # retrain some image sets, e.g. "only t25,t26", keeping the other records
        sets = sys.argv[2].split(",")
        build_images()
        path = os.path.join(HERE, "training.json")
        records = [r for r in json.load(open(path)) if r["image_set"] not in sets]
        records += [train(*t) for t in TRAININGS if t[0] in sets]
        json.dump(records, open(path, "w"), indent=1)
        bake_images(sets)
        sys.exit()
    build_images()
    migrate_heavy()
    records = [train(*t) for t in TRAININGS]
    json.dump(records, open(os.path.join(HERE, "training.json"), "w"), indent=1)
    bake_images()
    signal_run()
