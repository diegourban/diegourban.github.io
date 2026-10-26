#!/bin/bash
# Counts classes loaded from CDS archives during startup, default CDS vs AppCDS, on both demo apps.
# ZGC, -Xlog:class+load until Spring's context refresh. Small app: cdsdemo:gcs with /app/app-zgc.jsa
# (the archive used for the ZGC rows of results_gc.json). Heavy app: heavydemo:trained with /app/app.jsa.
# The heavy app needs Postgres: a temporary container named appcds-cov-pg is started and removed.
# Its first boot runs the Flyway migrations, so it is not counted. Writes coverage.json next to this script.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
NET=appcds-cov
cleanup() { docker rm -f appcds-cov-pg >/dev/null 2>&1 || true; docker network rm "$NET" >/dev/null 2>&1 || true; }
trap cleanup EXIT

count() { # image jar [java flags...] -- [docker args...] -> "shared loaded"
  local image="$1" jar="$2"; shift 2
  local flags=() dargs=()
  while [ "$1" != "--" ]; do flags+=("$1"); shift; done; shift
  dargs=("$@")
  docker run --rm "${dargs[@]}" --entrypoint java "$image" -XX:+UseZGC ${flags[@]+"${flags[@]}"} \
    -Xlog:class+load=info:file=/dev/stdout -Dspring.context.exit=onRefresh -Dspring.main.banner-mode=off \
    -jar "$jar" 2>&1 | awk '/\[class,load\]/ { total++ } /source: shared objects file/ { shared++ } END { print shared + 0, total + 0 }'
}

SJ=/app/cdsdemo-0.0.1-SNAPSHOT.jar
HJ=/app/heavydemo-0.0.1-SNAPSHOT.jar
{
  for i in 1 2 3; do echo "small default $(count cdsdemo:gcs $SJ -- --network none)"; done
  for i in 1 2 3; do echo "small appcds $(count cdsdemo:gcs $SJ -XX:SharedArchiveFile=/app/app-zgc.jsa -- --network none)"; done

  docker network create "$NET" >/dev/null
  docker run -d --name appcds-cov-pg --network "$NET" --network-alias pg -e POSTGRES_DB=heavy \
    -e POSTGRES_USER=heavy -e POSTGRES_PASSWORD=benchonly postgres:17.6 >/dev/null
  for i in $(seq 1 30); do docker exec appcds-cov-pg pg_isready -U heavy -d heavy -q && break; sleep 1; done
  count heavydemo:trained $HJ -- --network "$NET" >/dev/null   # applies the migrations
  for i in 1 2; do echo "heavy default $(count heavydemo:trained $HJ -- --network "$NET")"; done
  for i in 1 2; do echo "heavy appcds $(count heavydemo:trained $HJ -XX:SharedArchiveFile=/app/app.jsa -- --network "$NET")"; done
} | tee /dev/stderr | python3 -c "
import json, statistics as st, sys
rows = [l.split() for l in sys.stdin if l.strip()]
out = {'method': 'ZGC; -Xlog:class+load until Spring context refresh; median of runs', 'apps': {}}
for app in ('small', 'heavy'):
    for v in ('default', 'appcds'):
        rs = [(int(r[2]), int(r[3])) for r in rows if r[0] == app and r[1] == v]
        out['apps'].setdefault(app, {})[v] = {'shared': st.median(s for s, _ in rs), 'loaded': st.median(l for _, l in rs), 'runs': rs}
json.dump(out, open('$HERE/coverage.json', 'w'), indent=1)
for app, d in out['apps'].items():
    print(app, {v: (x['shared'], x['loaded']) for v, x in d.items()})
"
