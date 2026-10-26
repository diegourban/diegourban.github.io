#!/bin/bash
# Measures what AppCDS adds to a build: the training run's wall-clock time and the archive's size.
# The training run is the exact step the benchmark images run (ZGC, -XX:ArchiveClassesAtExit,
# Spring stopping at context refresh), executed on the untrained base images from benchmarks/default-cds.
# The heavy app needs Postgres: a temporary container named appcds-cost-pg is started and removed.
# Writes build_cost.json next to this script.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$(mktemp -d)"
NET=appcds-cost
cleanup() { docker rm -f appcds-cost-pg >/dev/null 2>&1 || true; docker network rm "$NET" >/dev/null 2>&1 || true; rm -rf "$OUT"; }
trap cleanup EXIT

train() { # label image jar runs [docker args...]
  local label="$1" image="$2" jar="$3" runs="$4"; shift 4
  for i in $(seq 1 "$runs"); do
    rm -f "$OUT/app.jsa"
    local t0 t1
    t0=$(python3 -c 'import time; print(time.time())')
    docker run --rm "$@" -v "$OUT:/out" --entrypoint java "$image" -XX:+UseZGC \
      -XX:ArchiveClassesAtExit=/out/app.jsa -Dspring.context.exit=onRefresh -jar "$jar" >/dev/null 2>&1
    t1=$(python3 -c 'import time; print(time.time())')
    echo "$label $i $(python3 -c "print(round($t1-$t0, 2))") $(stat -f %z "$OUT/app.jsa")"
  done
}

{
  # Baseline: the same container start and Spring boot without writing an archive.
  for i in 1 2 3 4 5; do
    t0=$(python3 -c 'import time; print(time.time())')
    docker run --rm --network none --entrypoint java cdsdemo:base -XX:+UseZGC \
      -Dspring.context.exit=onRefresh -jar /app/cdsdemo-0.0.1-SNAPSHOT.jar >/dev/null 2>&1
    t1=$(python3 -c 'import time; print(time.time())')
    echo "small_boot_only $i $(python3 -c "print(round($t1-$t0, 2))") 0"
  done
  train small_training cdsdemo:base /app/cdsdemo-0.0.1-SNAPSHOT.jar 5 --network none

  docker network create "$NET" >/dev/null
  docker run -d --name appcds-cost-pg --network "$NET" --network-alias pg -e POSTGRES_DB=heavy \
    -e POSTGRES_USER=heavy -e POSTGRES_PASSWORD=benchonly postgres:17.6 >/dev/null
  for i in $(seq 1 30); do docker exec appcds-cost-pg pg_isready -U heavy -d heavy -q && break; sleep 1; done
  # First boot applies the Flyway migrations; not counted.
  docker run --rm --network "$NET" --entrypoint java heavydemo:base -XX:+UseZGC \
    -Dspring.context.exit=onRefresh -jar /app/heavydemo-0.0.1-SNAPSHOT.jar >/dev/null 2>&1
  train heavy_training heavydemo:base /app/heavydemo-0.0.1-SNAPSHOT.jar 3 --network "$NET"
} | tee "$OUT/raw.txt"

python3 - "$OUT/raw.txt" "$HERE/build_cost.json" <<'PY'
import json, statistics as st, sys
rows = [l.split() for l in open(sys.argv[1]) if l.strip()]
def med(label, col):
    return st.median(float(r[col]) for r in rows if r[0] == label)
res = {
    "method": "docker run of the training step on the untrained base image; wall-clock seconds; archive size in bytes",
    "small_app": {"boot_only_s": med("small_boot_only", 2), "training_s": med("small_training", 2),
                  "archive_bytes": int(med("small_training", 3))},
    "heavy_app": {"training_s": med("heavy_training", 2), "archive_bytes": int(med("heavy_training", 3))},
    "raw": [{"label": r[0], "run": int(r[1]), "seconds": float(r[2]), "archive_bytes": int(r[3])} for r in rows],
}
json.dump(res, open(sys.argv[2], "w"), indent=1)
print(json.dumps({k: v for k, v in res.items() if k != "raw"}, indent=1))
PY
