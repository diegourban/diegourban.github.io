#!/bin/bash
# Counts how many of the classes loaded during startup come from the default CDS archive.
# Each app boots with ZGC and default CDS (no -Xshare flag), logs every class load, and
# exits right after Spring's context refresh. Writes coverage.json next to this script.
#
# Needs the benchmark images (cdsdemo:trained, heavydemo:trained). The heavy app needs
# Postgres reachable as "pg" on the Docker network given as $1 (default: cdsbench).
# Its first boot runs the Flyway migrations, so only boots 2 and 3 are counted.
set -euo pipefail
NET="${1:-cdsbench}"
HERE="$(cd "$(dirname "$0")" && pwd)"

count() { # image jar [docker args...] -> "shared total"
  local image="$1" jar="$2"; shift 2
  docker run --rm "$@" --entrypoint java "$image" -XX:+UseZGC \
    -Xlog:class+load=info:file=/dev/stdout -Dspring.context.exit=onRefresh \
    -Dspring.main.banner-mode=off -jar "$jar" 2>&1 |
    awk '/\[class,load\]/ { total++ } /source: shared objects file/ { shared++ } END { print shared + 0, total + 0 }'
}

small=(); heavy=()
for i in 1 2 3; do small+=("$(count cdsdemo:trained /app/cdsdemo-0.0.1-SNAPSHOT.jar --network none)"); done
count heavydemo:trained /app/heavydemo-0.0.1-SNAPSHOT.jar --network "$NET" >/dev/null   # runs migrations
for i in 1 2; do heavy+=("$(count heavydemo:trained /app/heavydemo-0.0.1-SNAPSHOT.jar --network "$NET")"); done

python3 - "$HERE/coverage.json" "${small[@]}" -- "${heavy[@]}" <<'PY'
import json, sys
out, args = sys.argv[1], sys.argv[2:]
cut = args.index("--")
runs = lambda xs: [dict(zip(("shared", "loaded"), map(int, x.split()))) for x in xs]
json.dump({"method": "default CDS, ZGC, -Xlog:class+load until Spring context refresh",
           "small_app": runs(args[:cut]), "heavy_app": runs(args[cut + 1:])}, open(out, "w"), indent=1)
print(open(out).read())
PY
