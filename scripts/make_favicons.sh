#!/bin/bash
# Renders every favicon PaperMod links (head.html) into static/, from scripts/favicon.svg:
# favicon.ico (48px), favicon-16x16.png, favicon-32x32.png, apple-touch-icon.png (180px)
# and safari-pinned-tab.svg (single-color bars). Needs headless Chrome and macOS sips.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$HERE/../static"
CHROME="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

cat > "$TMP/page.html" <<HTML
<!doctype html><html><head><style>html,body{margin:0;background:transparent}img{display:block;width:512px;height:512px}</style></head>
<body><img src="file://$HERE/favicon.svg"></body></html>
HTML
"$CHROME" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=1 \
  --default-background-color=00000000 --window-size=512,512 \
  --screenshot="$TMP/512.png" "file://$TMP/page.html" 2>/dev/null

mkdir -p "$OUT"
sips -z 180 180 "$TMP/512.png" --out "$OUT/apple-touch-icon.png" >/dev/null
sips -z 32 32 "$TMP/512.png" --out "$OUT/favicon-32x32.png" >/dev/null
sips -z 16 16 "$TMP/512.png" --out "$OUT/favicon-16x16.png" >/dev/null
sips -z 48 48 "$TMP/512.png" --out "$TMP/48.png" >/dev/null
sips -s format ico "$TMP/48.png" --out "$OUT/favicon.ico" >/dev/null

# Safari pinned tab: one color, no background; Safari tints it with the mask-icon color.
sed -e '/<rect width="512"/d' -e 's/fill="#ffffff"/fill="#000000"/' -e '/<!--/d' "$HERE/favicon.svg" > "$OUT/safari-pinned-tab.svg"

ls -la "$OUT" | grep -E 'favicon|apple-touch|safari'
