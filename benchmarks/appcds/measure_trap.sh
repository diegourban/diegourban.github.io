#!/bin/bash
# Measures what happens when an AppCDS archive is used with different GC flags than it was trained with.
# Uses cdsdemo:gcs from benchmarks/default-cds (one archive per GC: /app/app-<gc>.jsa, trained with that GC).
# For each case it records: classes loaded from the archive, whether `java -version` still says "sharing",
# whether anything is printed without -Xlog, what -Xlog:cds says, and whether -Xshare:on refuses to start.
# Writes trap.json next to this script.
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
IMAGE=cdsdemo:gcs
JAR=/app/cdsdemo-0.0.1-SNAPSHOT.jar
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

run_case() { # name gc_flag archive
  local name="$1" gc="$2" jsa="$3"
  docker run --rm --network none --entrypoint java "$IMAGE" $gc -XX:SharedArchiveFile="$jsa" \
    -Xlog:class+load=info:file=/dev/stdout -Dspring.context.exit=onRefresh -Dspring.main.banner-mode=off \
    -jar "$JAR" > "$TMP/$name.load" 2>&1
  docker run --rm --network none --entrypoint java "$IMAGE" $gc -XX:SharedArchiveFile="$jsa" \
    -Dspring.context.exit=onRefresh -Dspring.main.banner-mode=off -Dlogging.level.root=OFF \
    -jar "$JAR" > "$TMP/$name.plain" 2>&1
  docker run --rm --network none --entrypoint java "$IMAGE" $gc -XX:SharedArchiveFile="$jsa" \
    -Xlog:cds -version > "$TMP/$name.cds" 2>&1
  docker run --rm --network none --entrypoint java "$IMAGE" $gc -XX:SharedArchiveFile="$jsa" \
    -version > "$TMP/$name.version" 2>&1
  # -Xshare:on must run with the app's real classpath; with -version alone every AppCDS archive mismatches.
  docker run --rm --network none --entrypoint java "$IMAGE" $gc -XX:SharedArchiveFile="$jsa" \
    -Xshare:on -Dspring.context.exit=onRefresh -Dspring.main.banner-mode=off -Dlogging.level.root=OFF \
    -jar "$JAR" > "$TMP/$name.on" 2>&1
  echo "$?" > "$TMP/$name.on.exit"
  # The same check with -version instead of the app: no app classpath, so even a matched archive is rejected.
  docker run --rm --network none --entrypoint java "$IMAGE" $gc -XX:SharedArchiveFile="$jsa" \
    -Xshare:on -version > "$TMP/$name.onversion" 2>&1
  echo "$?" > "$TMP/$name.onversion.exit"
}

run_case matched_zgc -XX:+UseZGC /app/app-zgc.jsa
run_case matched_g1 -XX:+UseG1GC /app/app-g1.jsa
run_case trap_zgc_archive_g1_run -XX:+UseG1GC /app/app-zgc.jsa
run_case trap_g1_archive_zgc_run -XX:+UseZGC /app/app-g1.jsa
# Compact object headers change the object layout too: a G1 archive run with them turned on.
run_case trap_g1_archive_g1_compact_headers "-XX:+UseG1GC -XX:+UseCompactObjectHeaders" /app/app-g1.jsa
# Control: a different GC with the same layout (compressed oops on) keeps the archive.
run_case control_g1_archive_parallel_run -XX:+UseParallelGC /app/app-g1.jsa

python3 - "$TMP" "$HERE/trap.json" <<'PY'
import json, os, re, sys
tmp, out = sys.argv[1:]
cases = {}
for name in ("matched_zgc", "matched_g1", "trap_zgc_archive_g1_run", "trap_g1_archive_zgc_run", "trap_g1_archive_g1_compact_headers", "control_g1_archive_parallel_run"):
    load = open(os.path.join(tmp, name + ".load")).read()
    plain = open(os.path.join(tmp, name + ".plain")).read()
    cds = open(os.path.join(tmp, name + ".cds")).read()
    on = open(os.path.join(tmp, name + ".on")).read()
    version_lines = [l for l in open(os.path.join(tmp, name + ".version")).read().splitlines() if "VM " in l and "(build" in l]
    cases[name] = {
        "shared_classes": load.count("source: shared objects file"),
        "loaded_classes": len(re.findall(r"\[class,load\]", load)),
        "jvm_messages_without_xlog": [l for l in plain.splitlines() if re.search(r"warning|error|CDS|shar", l, re.I)],
        "java_version_says_sharing": bool(version_lines) and "sharing" in version_lines[-1],
        "xlog_cds_warnings": [l.strip() for l in cds.splitlines() if re.search(r"\[(warning|error|info)\]\[cds\].*(Unable|disabled|mismatch|different|fail)", l, re.I)],
        "xshare_on_exit_code": int(open(os.path.join(tmp, name + ".on.exit")).read().strip()),
        "xshare_on_output": [l.strip() for l in on.splitlines() if l.strip()][:3],
        "xshare_on_with_version_exit_code": int(open(os.path.join(tmp, name + ".onversion.exit")).read().strip()),
        "xshare_on_with_version_output": [l.strip() for l in open(os.path.join(tmp, name + ".onversion")).read().splitlines() if "cds" in l][:2],
        "jdk_version": (version_lines[-1].strip() if version_lines else ""),
    }
json.dump({"image": "cdsdemo:gcs", "method": "archive trained with one GC, run with another; Spring stops at context refresh", "cases": cases},
          open(out, "w"), indent=1)
print(json.dumps(cases, indent=1))
PY
