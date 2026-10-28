#!/bin/bash
# Single runs behind the claims the review asked to be recorded. Writes review_evidence.txt next to this script.
#  1. JDK 25: the AllowArchivingWithJavaAgent warning on a production start with the agent-trained archive.
#  2. JDK 26: the flag no longer exists, and a CDS dump refuses any agent.
#  3. JDK 26: why AOT cache training with the agent fails (java.lang.Class excluded).
#  4. JDK 25: why the cache trained without the agent loads nothing (the agent adds java.instrument).
#  5. JDK 25: the boot classpath warning, and how many classes the AppCDS dump with the agent skips.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
JAR=/app/cdsdemo-0.0.1-SNAPSHOT.jar
AGENT="-javaagent:/app/otel.jar -Dotel.traces.exporter=none -Dotel.metrics.exporter=none -Dotel.logs.exporter=none"
ALLOW="-XX:+UnlockDiagnosticVMOptions -XX:+AllowArchivingWithJavaAgent"
run() { docker run --rm --cpus=2 -m 1g --entrypoint java "$@" 2>&1; }
pick() { grep -m "${2:-3}" -E "$1" | sed -E 's/^\[[^]]*\]//' | cut -c1-200; }

{
  echo "== 1. JDK 25.0.3: production start with the agent-trained AppCDS archive and the flag"
  run aotbench:small -XX:+UseG1GC $AGENT $ALLOW -XX:SharedArchiveFile=/app/caches/appcds-g1-otel.jsa \
    -Dspring.context.exit=onRefresh -jar $JAR | pick "AllowArchivingWithJavaAgent|testing purposes"
  echo "== 2a. JDK 26.0.2.1: the flag"
  run aotbench:t26-base $ALLOW -version | pick "AllowArchivingWithJavaAgent|Unrecognized|Error"
  echo "== 2b. JDK 26.0.2.1: AppCDS dump with the agent"
  run aotbench:t26-base -XX:+UseG1GC $AGENT -XX:ArchiveClassesAtExit=/tmp/x.jsa -Dspring.context.exit=onRefresh -jar $JAR \
    | pick "JVMTI|agent|Error occurred"
  echo "== 3. JDK 26.0.2.1: AOT cache training with the agent (-Xlog:aot)"
  run aotbench:t26-base -XX:+UseG1GC $AGENT -XX:AOTCacheOutput=/tmp/x.aot -Xlog:aot=info -Dspring.context.exit=onRefresh -jar $JAR \
    | pick "Critical class|cannot be written|fatal error|Problematic" 4
  echo "== 3b. JDK 26.0.2.1: the same with AOT class linking off (-XX:-AOTClassLinking), which gets past the crash"
  run aotbench:t26-base -XX:+UseG1GC $AGENT -XX:-AOTClassLinking -XX:AOTCacheOutput=/tmp/x.aot -Xlog:aot=info -Dspring.context.exit=onRefresh -jar $JAR \
    | pick "Critical class|cannot be written|fatal error" 3
  echo "== 3c. JDK 25.0.3: core JDK classes the agent rewrites, skipped by AOT cache training"
  run aotbench:small -XX:+UseG1GC $AGENT -XX:-AOTClassLinking -XX:AOTCacheOutput=/tmp/x.aot -Dspring.context.exit=onRefresh -jar $JAR \
    | grep -o -E "Skipping (java/lang/Class|java/lang/ClassLoader|java/lang/Thread|jdk/internal/loader/BuiltinClassLoader):" | sort -u
  echo "== 4. JDK 25.0.3: the cache trained without the agent, started with it (-Xlog:cds)"
  run aotbench:small -XX:+UseG1GC $AGENT -XX:AOTCache=/app/caches/aot-g1.aot -Xlog:cds=info -Dspring.context.exit=onRefresh -jar $JAR \
    | pick "addmods|full module graph|aot-linked" 3
  echo "== 5a. JDK 25.0.3: start with the agent and the agent-trained archive"
  run aotbench:small -XX:+UseG1GC $AGENT $ALLOW -XX:SharedArchiveFile=/app/caches/appcds-g1-otel.jsa \
    -Dspring.context.exit=onRefresh -jar $JAR | pick "bootstrap classpath has been appended" 1
  echo "== 5b. JDK 25.0.3: AppCDS dump with the agent (classes skipped as rewritten)"
  echo "skipped: $(run aotbench:small -XX:+UseG1GC $AGENT $ALLOW -XX:ArchiveClassesAtExit=/tmp/x.jsa -Dspring.context.exit=onRefresh -jar $JAR \
    | grep -c 'From ClassFileLoadHook')"
} | tee "$HERE/review_evidence.txt"
