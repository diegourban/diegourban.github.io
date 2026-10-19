#!/bin/bash
# Renders the site's default social preview card (static/site-card.png, 1200x630) with headless Chrome.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
CHROME="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"
mkdir -p "$HERE/../static"
"$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
  --virtual-time-budget=2000 --window-size=1200,630 \
  --screenshot="$HERE/../static/site-card.png" "file://$HERE/site-card.html" 2>/dev/null
echo "wrote static/site-card.png"
