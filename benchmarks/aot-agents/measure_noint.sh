#!/bin/bash
# Can a cache be trained with the agent if it stops rewriting JDK classes? Writes noint_evidence.txt next to this script.
# JDK 25.0.3, AOT cache training (one step, -XX:AOTCacheOutput) with OpenTelemetry's instrumentations that rewrite
# JDK classes switched off, then a start with that cache and the same agent settings.
#  1. internal-reflection and internal-class-loader off.
#  2. executors off as well.
# A control (0) starts the app with attempt 2's settings and no cache, to separate the agent's failure from the cache's.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
JAR=/app/cdsdemo-0.0.1-SNAPSHOT.jar
AGENT="-javaagent:/app/otel.jar -Dotel.traces.exporter=none -Dotel.metrics.exporter=none -Dotel.logs.exporter=none"
OFF1="-Dotel.instrumentation.internal-reflection.enabled=false -Dotel.instrumentation.internal-class-loader.enabled=false"
OFF2="$OFF1 -Dotel.instrumentation.executors.enabled=false"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
run() { docker run --rm --cpus=2 -m 1g -v "$OUT:/out" --entrypoint java "$@" 2>&1; }

attempt() {  # $1 label, $2 instrumentation flags
  rm -f "$OUT/c.aot"
  echo "== $1: training"
  run aotbench:small -XX:+UseG1GC $AGENT $2 -XX:AOTCacheOutput=/out/c.aot -Dspring.context.exit=onRefresh -jar $JAR \
    | grep -m 4 -o -E "Skipping (java/lang/Class|java/lang/ClassLoader|jdk/internal/loader/BuiltinClassLoader):|SIGSEGV|Problematic frame:|NoClassDefFoundError: [^ ]+|AOTCache creation is complete.*" | sort -u
  if [ -f "$OUT/c.aot" ]; then
    echo "cache written: $(wc -c < "$OUT/c.aot" | tr -d ' ') bytes"
    echo "== $1: start with the cache"
    start -XX:AOTCache=/out/c.aot -Xlog:aot=info $2
  else
    echo "cache written: no"
  fi
}

start() {  # starts the app up to Spring's context refresh and reports how it ended
  local out
  out="$(run aotbench:small -XX:+UseG1GC $AGENT "$@" -Dspring.context.exit=onRefresh -jar $JAR; echo "exit code: $?")"
  echo "$out" | grep -m 4 -o -E 'Using AOT-linked classes: [a-z]+|Exception in thread "main" [^ ]+: [^ ]+|Started [A-Za-z]+ in [0-9.]+ seconds|exit code: [0-9]+' | sort -u
}

{
  echo "== 0. control: the same settings as attempt 2, no cache at all"
  start $OFF2
  attempt "1. internal-reflection and internal-class-loader off" "$OFF1"
  attempt "2. executors off as well" "$OFF2"
} | tee "$HERE/noint_evidence.txt"
