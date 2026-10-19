#!/bin/bash
# Renders the post's social preview card (content/posts/default-cds/cover.png, 1200x630) from
# cover.html and charts.json, with headless Chrome. Run make_charts.py first.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
POST="$HERE/../../content/posts/default-cds"
CHROME="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"

TMP="$HERE/.cover.tmp.html"
trap 'rm -f "$TMP"' EXIT
python3 - "$HERE/cover.html" "$POST/charts.json" "$TMP" <<'PY'
import sys
html, data, out = sys.argv[1:]
open(out, "w").write(open(html).read().replace("/*DATA*/null", open(data).read()))
PY

"$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
  --virtual-time-budget=3000 --window-size=1200,630 --screenshot="$POST/cover.png" "file://$TMP" 2>/dev/null
echo "wrote content/posts/default-cds/cover.png"
