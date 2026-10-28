#!/bin/bash
# How often does AOT cache training with the OpenTelemetry agent produce a cache? Repeats the training
# (up to the context refresh) N times per JDK image and counts: cache written, crash (hs_err), other.
# A written cache is then booted once with the agent to see whether it loads. Writes probe_repeat.txt.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
N=${1:-5}
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT
JAR=/app/cdsdemo-0.0.1-SNAPSHOT.jar
AGENT="-javaagent:/app/otel.jar -Dotel.traces.exporter=none -Dotel.metrics.exporter=none -Dotel.logs.exporter=none"

{
for image in aotbench:small aotbench:t25-base aotbench:t26-base; do
  written=0; crashed=0; loaded=""
  for i in $(seq "$N"); do
    rm -rf "$OUT"/*
    log=$(docker run --rm --cpus=2 -m 1g -v "$OUT:/out" -w /out --entrypoint java "$image" \
      -XX:+UseG1GC $AGENT -XX:AOTCacheOutput=/out/c.aot -Dspring.context.exit=onRefresh -jar $JAR 2>&1)
    if [ -s "$OUT/c.aot" ]; then
      written=$((written + 1))
      if [ -z "$loaded" ]; then
        run=$(docker run --rm --cpus=2 -m 1g -v "$OUT:/out" --entrypoint java "$image" -XX:+UseG1GC $AGENT \
          -XX:AOTCache=/out/c.aot -Xlog:class+load=info:file=/dev/stdout -Dspring.context.exit=onRefresh -jar $JAR 2>&1)
        loaded="$(echo "$run" | grep -c 'source: shared objects file') of $(echo "$run" | grep -c '\[class,load\]') classes from it"
      fi
    elif echo "$log" | grep -q "A fatal error has been detected"; then
      crashed=$((crashed + 1))
    fi
  done
  version=$(docker run --rm --entrypoint java "$image" -version 2>&1 | tail -1 | grep -o "Temurin-[0-9.+]*")
  echo "$version: $N trainings, cache written $written, JVM crash $crashed${loaded:+, first cache loaded $loaded}"
done
} | tee "$HERE/probe_repeat.txt"
