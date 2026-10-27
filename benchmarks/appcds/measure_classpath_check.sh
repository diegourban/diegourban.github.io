#!/bin/bash
# Does AppCDS notice when the JAR it was trained on changes? Builds the recipe image, then two variants that
# replace only the application JAR: one with a new modification time (same bytes), one repacked without
# compression (same entries, different size) with the original modification time. Counts classes from the archive in each.
# Writes classpath_check.txt next to this script.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
JAR=/app/cdsdemo-0.0.1-SNAPSHOT.jar
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
docker build -q -t appcds-recipe:test "$HERE/recipe" >/dev/null || exit 1

# Repacks /x.jar with the same entries, stored uncompressed, then puts the original mtime back.
cat > "$TMP/repack.py" <<'EOF'
import os, shutil, zipfile
shutil.copy2("/x.jar", "/ref")
with zipfile.ZipFile("/ref") as src, zipfile.ZipFile("/x.jar", "w", zipfile.ZIP_STORED) as dst:
    for entry in src.infolist():
        dst.writestr(entry.filename, src.read(entry.filename))
st = os.stat("/ref")
os.utime("/x.jar", (st.st_atime, st.st_mtime))
print("size", st.st_size, "->", os.stat("/x.jar").st_size)
EOF

variant() {  # $1 = tag, $2 = command run on /x.jar
  cat > "$TMP/Dockerfile" <<EOF
FROM appcds-recipe:test AS recipe
FROM python:3.12-alpine AS edit
COPY --from=recipe $JAR /x.jar
COPY repack.py /repack.py
RUN $2
FROM appcds-recipe:test
COPY --from=edit /x.jar $JAR
EOF
  docker build -q -t "appcds-recipe:$1" "$TMP" >/dev/null
}
variant mtime "touch -d '2030-01-01 00:00:00' /x.jar"
variant size  "python3 /repack.py"

count() {
  docker run --rm --network none \
    -e "JDK_JAVA_OPTIONS=-Xlog:class+load=info:file=/dev/stdout -Dspring.context.exit=onRefresh -Dspring.main.banner-mode=off" "$1" 2>&1 \
    | awk '/source: shared objects file/ { s++ } /\[class,load\]/ { t++ } /\[warning\]\[cds/ && !m { m=$0 } /^Error:/ && !e { e=$0 } END { print s+0, "of", t+0, (m ? "| first warning: " m : ""), e }'
}
{
  echo "jdk: $(docker run --rm --entrypoint java appcds-recipe:test -version 2>&1 | tail -1)"
  echo "as built:                       $(count appcds-recipe:test)"
  echo "same JAR bytes, new mtime:      $(count appcds-recipe:mtime)"
  echo "repacked JAR, same mtime:       $(count appcds-recipe:size)"
} | tee "$HERE/classpath_check.txt"
docker image rm appcds-recipe:test appcds-recipe:mtime appcds-recipe:size >/dev/null
