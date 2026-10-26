#!/bin/bash
# Records how the JDK 25 AOT cache behaves with ZGC on the benchmark image (the post's "And the AOT cache?" section):
# create the cache in one step, then run with -XX:AOTMode=on and count classes loaded from it.
# Writes aot_zgc.txt next to this script.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT
IMG=cdsdemo:gcs
JAR=/app/cdsdemo-0.0.1-SNAPSHOT.jar
{
  echo "image: $IMG ($(docker run --rm --entrypoint java "$IMG" -version 2>&1 | tail -1))"
  docker run --rm --network none -v "$OUT:/out" --entrypoint java "$IMG" -XX:+UseZGC \
    -XX:AOTCacheOutput=/out/app.aot -Xlog:aot=info:file=/out/create.log \
    -Dspring.context.exit=onRefresh -Dspring.main.banner-mode=off -jar "$JAR" > "$OUT/create.out" 2>&1
  echo "create exit: $?"
  grep -h -o -E 'ZGC is not supported|AOTCache creation is complete: [^ ]+ [0-9]+ bytes' "$OUT/create.log" "$OUT/create.out" | sort -u
  docker run --rm --network none -v "$OUT:/out" --entrypoint java "$IMG" -XX:+UseZGC \
    -XX:AOTCache=/out/app.aot -XX:AOTMode=on -Xlog:class+load=info:file=/out/run.log \
    -Dspring.context.exit=onRefresh -Dspring.main.banner-mode=off -Dlogging.level.root=OFF -jar "$JAR" > "$OUT/run.out" 2>&1
  echo "run with -XX:AOTMode=on exit: $?"
  echo "classes from the cache: $(grep -c 'source: shared objects file' "$OUT/run.log") of $(grep -c '\[class,load\]' "$OUT/run.log")"
} | tee "$HERE/aot_zgc.txt"
