#!/usr/bin/env python3
"""Draws the two-transaction interleaving from the check (checks.json, plainReadFirst on MySQL) as a sequence diagram:
content/posts/mysql-lock/sequence.svg. The post inlines it with the diagram shortcode, so colours come from the
site's theme variables (assets/css/extended/charts.css) and follow dark and light mode."""
import os
from xml.sax.saxutils import escape

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "..", "content", "posts", "mysql-lock", "sequence.svg")

# The post column is 720 px wide, so the drawing shows at 1:1 there and scales down on phones.
W = 720
X = {"A": 92, "DB": 360, "B": 628}
TOP, STEP = 96, 56

# (from, to, label, note, bad): one message per row, the note goes under the arrow.
STEPS = [
    ("A", "DB", "select customer", "A's snapshot is taken", False),
    ("A", "DB", "select event ... for update", "A gets the event lock", False),
    ("A", "DB", "count bookings", "0, so a seat is free", False),
    ("A", "DB", "insert booking", "not committed yet", False),
    ("B", "DB", "select customer", "B's snapshot is taken: no booking in it", True),
    ("B", "DB", "select event ... for update", "waits for A's lock", False),
    ("A", "DB", "commit", "A's booking is committed, lock released", False),
    ("DB", "B", "lock granted", "", False),
    ("B", "DB", "count bookings", "0 again, read from B's old snapshot", True),
    ("B", "DB", "insert booking", "2 bookings for 1 seat", True),
]


def y(i):
    return TOP + 30 + i * STEP


def main():
    h = y(len(STEPS) - 1) + 50
    out = [f'<svg viewBox="0 0 {W} {h}" xmlns="http://www.w3.org/2000/svg" class="cp-seq" aria-hidden="true">',
           "<defs>",
           '<marker id="seq-head" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">'
           '<path d="M0,0 L10,5 L0,10 z" class="cp-seq-head"/></marker>',
           '<marker id="seq-head-bad" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">'
           '<path d="M0,0 L10,5 L0,10 z" class="cp-seq-head-bad"/></marker>',
           "</defs>"]
    # lifelines and their headers
    for name, label in (("A", "Booking A"), ("DB", "MySQL"), ("B", "Booking B")):
        x = X[name]
        out.append(f'<line x1="{x}" y1="{TOP - 30}" x2="{x}" y2="{h - 10}" class="cp-seq-life"/>')
        out.append(f'<rect x="{x - 64}" y="{TOP - 70}" width="128" height="40" rx="6" class="cp-seq-box"/>')
        out.append(f'<text x="{x}" y="{TOP - 44}" text-anchor="middle" class="cp-t-lab">{label}</text>')
    # A holds the event lock from step 2 to its commit (step 7)
    out.append(f'<rect x="{X["A"] - 5}" y="{y(1)}" width="10" height="{y(6) - y(1)}" rx="2" class="cp-seq-hold"/>')
    out.append(f'<text x="{X["A"] - 14}" y="{(y(1) + y(6)) / 2}" text-anchor="end" class="cp-t-val2">holds</text>')
    out.append(f'<text x="{X["A"] - 14}" y="{(y(1) + y(6)) / 2 + 16}" text-anchor="end" class="cp-t-val2">the lock</text>')
    # B waits from its lock request (step 6) until the lock is granted (step 8)
    out.append(f'<line x1="{X["B"]}" y1="{y(5)}" x2="{X["B"]}" y2="{y(7)}" class="cp-seq-wait"/>')
    out.append(f'<text x="{X["B"] + 14}" y="{(y(5) + y(7)) / 2 + 4}" class="cp-t-val2">waits</text>')
    for i, (src, dst, label, note, bad) in enumerate(STEPS):
        x1, x2, yy = X[src], X[dst], y(i)
        pad = 8 if x2 > x1 else -8
        cls, head = ("cp-seq-msg cp-seq-bad", "seq-head-bad") if bad else ("cp-seq-msg", "seq-head")
        out.append(f'<line x1="{x1 + pad}" y1="{yy}" x2="{x2 - pad}" y2="{yy}" class="{cls}" marker-end="url(#{head})"/>')
        mid = (x1 + x2) / 2
        out.append(f'<text x="{mid}" y="{yy - 8}" text-anchor="middle" class="cp-seq-code">{escape(label)}</text>')
        if note:
            ncls = "cp-seq-note cp-seq-note-bad" if bad else "cp-seq-note"
            out.append(f'<text x="{mid}" y="{yy + 18}" text-anchor="middle" class="{ncls}">{escape(note)}</text>')
    out.append("</svg>")
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w") as f:
        f.write("\n".join(out) + "\n")
    print(f"wrote {OUT} ({W}x{h})")


if __name__ == "__main__":
    main()
