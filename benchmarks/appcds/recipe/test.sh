#!/bin/bash
# Builds the recipe image and counts archived classes: as built, and with Deployment-style JAVA_TOOL_OPTIONS
# overrides. Writes results.txt next to this script.
# Extra test flags go through JDK_JAVA_OPTIONS: arguments after the ENTRYPOINT's "-jar app.jar" would reach the app, not the JVM.
set -u
cd "$(dirname "$0")"
docker build -q -t appcds-recipe:test . >/dev/null && echo "build ok"
TEST_OPTS="-Xlog:class+load=info:file=/dev/stdout -Dspring.context.exit=onRefresh -Dspring.main.banner-mode=off"
count() {
  docker run --rm --network none -e "JDK_JAVA_OPTIONS=$TEST_OPTS" "$@" appcds-recipe:test 2>&1 \
    | awk '/source: shared objects file/ { s++ } /\[class,load\]/ { t++ } /Unable to use shared archive/ { w=1 } END { print s+0, "of", t+0, (w ? "(warning printed)" : "") }'
}
{
  echo "image JAVA_TOOL_OPTIONS=-XX:+UseZGC (training and runtime)"
  echo "as built:                                      $(count)"
  echo "Deployment JAVA_TOOL_OPTIONS=-XX:+UseG1GC:     $(count -e JAVA_TOOL_OPTIONS=-XX:+UseG1GC)"
  echo "Deployment JAVA_TOOL_OPTIONS='-XX:+UseZGC -Xmx512m': $(count -e 'JAVA_TOOL_OPTIONS=-XX:+UseZGC -Xmx512m')"
  echo "Deployment JAVA_TOOL_OPTIONS=-Xmx512m (only):  $(count -e JAVA_TOOL_OPTIONS=-Xmx512m)"
} | tee results.txt
docker image rm appcds-recipe:test >/dev/null && echo "image removed"
