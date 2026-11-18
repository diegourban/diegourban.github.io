#!/bin/bash
# Renders the LinkedIn card for the MySQL lock post (linkedin-card.html, 1200x630) with headless Chrome.
# Usage: make_linkedin_card.sh [output.png]    default: ~/Desktop/platformnotes-mysql-lock-linkedin.png
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="${1:-$HOME/Desktop/platformnotes-mysql-lock-linkedin.png}"
CHROME="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"
"$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 --blink-settings=preferredColorScheme=1 \
  --virtual-time-budget=3000 --window-size=1200,630 --screenshot="$OUT" "file://$HERE/linkedin-card.html" 2>/dev/null
echo "wrote $OUT"
