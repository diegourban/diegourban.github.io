#!/usr/bin/env python3
"""Single-run checks of how the JDK 25 AOT cache behaves when something is off. Writes oneoffs.json.

All on the small app with G1 and the cache trained by train.py (.caches/small/aot-g1.aot):
  - a missing, empty, truncated or stale cache, each with -XX:AOTMode=auto (the default) and =on;
  - mismatched runtime flags: ZGC instead of G1, and an extra -Xmx512m (a Deployment adding JVM options);
  - trying to extend a cache: a training run given an existing cache plus a new output;
  - the base image sizes (each cache adds its size from training.json).
"auto" must always start the app; the record says whether it did, how fast, how many classes came from the cache,
and the first AOT/CDS log lines.
"""
import json, os, re, shutil, subprocess, tempfile, time
import common as c

HERE = os.path.dirname(os.path.abspath(__file__))
CACHE = os.path.join(HERE, ".caches", "small", "aot-g1.aot")
CLASSLOG = "-Xlog:class+load=info:file=/dev/stdout"


def boot(image, flags, mounts=()):
    """Boots to Spring's context refresh and reports exit code, startup, classes from the cache and AOT/CDS lines."""
    args = ["docker", "run", "--rm", *c.LIMITS, *[x for m in mounts for x in ("-v", m)], "--entrypoint", "java",
            image, *flags, CLASSLOG, "-Dspring.context.exit=onRefresh", "-jar", c.SMALL_JAR]
    t0 = time.monotonic()
    p = subprocess.run(args, capture_output=True, text=True)
    seconds = time.monotonic() - t0
    out = p.stdout + p.stderr
    # onRefresh exits before Spring logs "Started", so exit code 0 is the sign the app came up.
    return {"exit_code": p.returncode, "started": p.returncode == 0, "wall_s_to_refresh": round(seconds, 2),
            "classes_from_cache": len(re.findall(r"source: shared objects file", out)),
            "classes_loaded": len(re.findall(r"\[class,load\]", out)),
            "jvm_lines": [l.strip()[:200] for l in out.splitlines() if "[class,load]" not in l and
                          re.search(r"\[(warning|error)\s*\]\[(aot|cds)|Error occurred|Unexpected exception|AOT cache", l)][:4]}


def stale_image():
    """aotbench:small with the application JAR's mtime changed, so its caches are stale."""
    d = tempfile.mkdtemp()
    with open(os.path.join(d, "Dockerfile"), "w") as f:
        f.write(f"FROM aotbench:small AS app\nFROM busybox:1.37.0 AS edit\nCOPY --from=app {c.SMALL_JAR} /x.jar\n"
                f"RUN touch -d '2030-01-01 00:00:00' /x.jar\nFROM aotbench:small\nCOPY --from=edit /x.jar {c.SMALL_JAR}\n")
    c.sh("docker", "build", "-q", "-t", "aotbench:stale", d)
    shutil.rmtree(d)
    return "aotbench:stale"


def main():
    version = subprocess.run(["docker", "run", "--rm", "--entrypoint", "java", "aotbench:small", "-version"], capture_output=True, text=True)
    res = {"jdk": version.stderr.strip().splitlines()[-1]}
    d = tempfile.mkdtemp(dir=os.path.join(HERE, ".caches"))
    open(os.path.join(d, "empty.aot"), "wb").close()
    data = open(CACHE, "rb").read()
    open(os.path.join(d, "truncated.aot"), "wb").write(data[: len(data) // 2])
    mount = f"{d}:/broken"
    g1 = "-XX:+UseG1GC"
    cases = {
        "intact": ("aotbench:small", [g1, "-XX:AOTCache=/app/caches/aot-g1.aot"]),
        "missing": ("aotbench:small", [g1, "-XX:AOTCache=/broken/missing.aot"]),
        "empty": ("aotbench:small", [g1, "-XX:AOTCache=/broken/empty.aot"]),
        "truncated": ("aotbench:small", [g1, "-XX:AOTCache=/broken/truncated.aot"]),
        "stale_jar_mtime": (stale_image(), [g1, "-XX:AOTCache=/app/caches/aot-g1.aot"]),
        "g1_cache_zgc_run": ("aotbench:small", ["-XX:+UseZGC", "-XX:AOTCache=/app/caches/aot-g1.aot"]),
        "g1_cache_plus_xmx512m": ("aotbench:small", [g1, "-Xmx512m", "-XX:AOTCache=/app/caches/aot-g1.aot"]),
    }
    for name, (image, flags) in cases.items():
        for mode in ("auto", "on"):
            res[f"{name}/{mode}"] = boot(image, flags + [f"-XX:AOTMode={mode}"], [mount])
            r = res[f"{name}/{mode}"]
            print(f"{name:24} {mode:4} exit {r['exit_code']} started {r['started']} {r['wall_s_to_refresh']}s "
                  f"cache {r['classes_from_cache']}/{r['classes_loaded']} {(r['jvm_lines'] or [''])[0][:90]}", flush=True)
    # Extending: an existing cache plus a new output in one training run.
    p = subprocess.run(["docker", "run", "--rm", *c.LIMITS, "-v", f"{d}:/out", "--entrypoint", "java", "aotbench:small",
                        g1, "-XX:AOTCache=/app/caches/aot-g1.aot", "-XX:AOTCacheOutput=/out/extended.aot",
                        "-Dspring.context.exit=onRefresh", "-jar", c.SMALL_JAR], capture_output=True, text=True)
    out = p.stdout + p.stderr
    res["extend_attempt"] = {"exit_code": p.returncode, "new_cache_written": os.path.exists(os.path.join(d, "extended.aot")),
                             "jvm_lines": [l.strip()[:220] for l in out.splitlines() if re.search(r"AOT|aot|Error", l)][:6]}
    print("extend:", json.dumps(res["extend_attempt"])[:400], flush=True)
    # JDK 26 on a jlink-built runtime without jdk.compiler (the Temurin JRE image): JDK-8381222.
    c.sh("docker", "build", "-q", "--build-arg", "JDK=26", "--build-arg", "FLAVOR=jre", "-t", "aotbench:t26jre-base",
         "-f", os.path.join(HERE, "Dockerfile.temurin"), HERE)
    subprocess.run(["docker", "run", "--rm", *c.LIMITS, "-v", f"{d}:/out", "--entrypoint", "java", "aotbench:t26jre-base",
                    g1, "-XX:AOTCacheOutput=/out/jre26.aot", "-Dspring.context.exit=onRefresh", "-jar", c.SMALL_JAR],
                   capture_output=True)
    for mode in ("auto", "on"):
        res[f"jdk26_jre_image/{mode}"] = boot("aotbench:t26jre-base", [g1, "-XX:AOTCache=/broken/jre26.aot", f"-XX:AOTMode={mode}"], [f"{d}:/broken"])
        r = res[f"jdk26_jre_image/{mode}"]
        print(f"jdk26_jre_image          {mode:4} exit {r['exit_code']} started {r['started']} {(r['jvm_lines'] or [''])[0][:90]}", flush=True)
    # aotbench:* bundle every cache for the benchmark; one deployed cache adds its own size (training.json).
    res["base_image_mb"] = {img: round(int(c.sh("docker", "image", "inspect", "-f", "{{.Size}}", img).strip()) / 1e6)
                            for img in ("cdsdemo:base", "heavydemo:base")}
    shutil.rmtree(d)
    json.dump(res, open(os.path.join(HERE, "oneoffs.json"), "w"), indent=1)
    print("base image MB:", res["base_image_mb"])


if __name__ == "__main__":
    main()
