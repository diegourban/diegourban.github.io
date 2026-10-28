#!/bin/bash
# Where does the AOT cache training with the OpenTelemetry agent crash? Each case trains up to the context
# refresh with the agent attached and reports the exit code, the crash frame and whether a cache was written.
# aarch64 only (Docker Desktop on an arm64 Mac); x86-64 wasn't tested.
# Writes probe_crash.txt next to this script.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT
JAR=/app/cdsdemo-0.0.1-SNAPSHOT.jar
AGENT="-javaagent:/app/otel.jar -Dotel.traces.exporter=none -Dotel.metrics.exporter=none -Dotel.logs.exporter=none"

probe() {  # label, platform, image, extra flags...
  local label=$1 platform=$2 image=$3; shift 3
  rm -f "$OUT"/*.aot "$OUT"/hs_err*
  local log
  log=$(docker run --rm --platform "$platform" --cpus=2 -m 2g -v "$OUT:/out" -w /out --entrypoint java "$image" \
    -XX:+UseG1GC $AGENT "$@" -XX:AOTCacheOutput=/out/c.aot -Dspring.context.exit=onRefresh -jar $JAR 2>&1)
  local code=$?
  local frame
  frame=$(echo "$log" | grep -m1 -A1 "Problematic frame" | tail -1 | cut -c1-110)
  local version
  version=$(echo "$log" | grep -m1 -o "JRE version: [^(]*" )
  printf '%-40s exit %-3s cache %-3s %s %s\n' "$label" "$code" "$([ -f "$OUT/c.aot" ] && echo yes || echo no)" "${frame:-no crash}" "$version"
}

{
  probe "JDK 25.0.3 distroless, arm64"             linux/arm64 aotbench:small
  probe "JDK 25.0.3, -XX:-AOTRecordTraining"       linux/arm64 aotbench:small -XX:+UnlockDiagnosticVMOptions -XX:-AOTRecordTraining
  probe "JDK 25.0.4 Temurin, arm64"                linux/arm64 aotbench:t25-base
  probe "JDK 26.0.2 Temurin, arm64"                linux/arm64 aotbench:t26-base
} | tee "$HERE/probe_crash.txt"
