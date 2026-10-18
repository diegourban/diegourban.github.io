import json, re, subprocess, sys, time, urllib.request, statistics, random

JAR = "/app/cdsdemo-0.0.1-SNAPSHOT.jar"
VARIANTS = {
    "xshare_off":        ("cdsdemo:trained", ["-Xshare:off"]),
    "cds_default":       ("cdsdemo:trained", []),
    "appcds":            ("cdsdemo:trained", ["-XX:SharedArchiveFile=/app/app.jsa"]),
}
N = int(sys.argv[1]) if len(sys.argv) > 1 else 7

def sh(*a):
    return subprocess.run(a, capture_output=True, text=True).stdout

def get(url):
    with urllib.request.urlopen(url, timeout=2) as r:
        return json.loads(r.read())

def run(name):
    image, flags = VARIANTS[name]
    port = random.randint(20000, 40000)
    cname = f"cds-{name}-{port}"
    cmd = ["docker", "run", "-d", "--name", cname, "--cpus=2", "-m", "1g", "-p", f"{port}:8080",
           "--entrypoint", "java", image, "-XX:+UseZGC", *flags, "-jar", JAR]
    t0 = time.monotonic()
    subprocess.run(cmd, capture_output=True, check=True)
    ready = None
    while time.monotonic() - t0 < 120:
        try:
            if get(f"http://localhost:{port}/actuator/health")["status"] == "UP":
                ready = time.monotonic() - t0
                break
        except Exception:
            pass
        time.sleep(0.05)
    time.sleep(1)
    logs = sh("docker", "logs", cname) + sh("docker", "logs", cname)
    m = re.search(r"Started \w+ in ([\d.]+) seconds \(process running for ([\d.]+)\)", logs)
    proc = sh("docker", "run", "--rm", f"--pid=container:{cname}", "busybox:1.37.0", "sh", "-c",
              "cat /proc/1/status; echo STAT; cat /proc/1/stat")
    kb = lambda k: int(re.search(rf"{k}:\s+(\d+) kB", proc).group(1)) / 1024
    stat = proc.split("STAT")[1].split()
    cpu_s = (int(stat[13]) + int(stat[14])) / 100.0
    ms = get(f"http://localhost:{port}/actuator/metrics/jvm.memory.used?tag=id:Metaspace")["measurements"][0]["value"] / 2**20
    cls = get(f"http://localhost:{port}/actuator/metrics/jvm.classes.loaded")["measurements"][0]["value"]
    subprocess.run(["docker", "rm", "-f", cname], capture_output=True)
    return dict(variant=name, started=float(m.group(1)), process=float(m.group(2)), ready_wall=ready,
                cpu_s=cpu_s, rss=kb("VmRSS"), rss_anon=kb("RssAnon"), rss_file=kb("RssFile"),
                metaspace=ms, classes=cls)

results = []
for i in range(N):
    order = list(VARIANTS); random.shuffle(order)
    for v in order:
        r = run(v); results.append(r)
        print(json.dumps(r), flush=True)
json.dump(results, open(sys.argv[2] if len(sys.argv) > 2 else "results.json", "w"), indent=1)
