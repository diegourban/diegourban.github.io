#!/bin/bash
# Renders the post's cover, used for the link preview and as the LinkedIn image
# (content/posts/mysql-lock/cover.png, 1200x630), from cover.html with headless Chrome.
# The chart's points in cover.html are copied from charts.json "arrivals"; update them if make_charts.py changes.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
POST="$HERE/../../content/posts/mysql-lock"
CHROME="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"
"$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 --blink-settings=preferredColorScheme=1 \
  --virtual-time-budget=3000 --window-size=1200,630 --screenshot="$POST/cover.png" "file://$HERE/cover.html" 2>/dev/null
echo "wrote content/posts/mysql-lock/cover.png"
