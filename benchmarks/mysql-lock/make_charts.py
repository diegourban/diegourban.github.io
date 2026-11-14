#!/usr/bin/env python3
"""Chart and table data for the MySQL lock post: content/posts/mysql-lock/charts.json.

- "arrivals": share of events overbooked as the booking rate per event rises, the 3 rounds pooled (300 events per rate).
- "flash": seats sold per event when c bookers keep trying until the event says it's full (200 events x 3 rounds).
- "rates": the arrival numbers as a table, with Wilson 95% intervals, so a 0 comes with its upper bound.
- "checks": the two-transaction checks on a 1-seat event, from checks.json.
Every number in the post comes from here, from results.jsonl or from checks.json.
"""
import collections, json, math, os, statistics as st

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "..", "content", "posts", "mysql-lock", "charts.json")
RATES = [10, 20, 50, 100, 200, 500, 1000]
CONC = [2, 5, 10, 20, 50]
FIXES = [("mysql", "lockFirst"), ("mysql", "readCommitted"), ("mysql", "counter"), ("postgresql", "plainReadFirst")]
RED, AMBER = "#d93636", "#d4a20a"


def wilson(k, n, z=1.96):
    p = k / n
    centre = (p + z * z / (2 * n)) / (1 + z * z / n)
    half = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / (1 + z * z / n)
    return max(0.0, centre - half), min(1.0, centre + half)


def pct(x):
    return round(100 * x, 1)


def main():
    rows = [json.loads(l) for l in open(os.path.join(HERE, "results.jsonl"))]
    over = collections.defaultdict(lambda: [0, 0])  # (db, variant, rate) -> [overbooked, events]
    sold = collections.defaultdict(collections.Counter)  # (db, variant, c) -> seats sold -> events
    req_ms = collections.defaultdict(list)
    for r in rows:
        k = (r["db"], r["variant"])
        if r["mode"] == "arrivals":
            over[k + (r["rate"],)][0] += r["overbooked"]
            over[k + (r["rate"],)][1] += r["events"]
            req_ms[k].append(r["meanRequestMs"])
        else:
            for seats, n in r["sold"].items():
                sold[k + (r["concurrency"],)][int(seats)] += n
    share = lambda db, v, rate: over[(db, v, rate)][0] / over[(db, v, rate)][1]
    fix_max = lambda rate: max(share(db, v, rate) for db, v in FIXES)

    arrivals = {
        "title": "Events overbooked as bookings arrive faster",
        "sub": ["10 seats, 15 booking requests per event arriving at random. MySQL 8.4 at REPEATABLE READ.",
                "300 events per rate (100 per round, 3 rounds)."],
        "x": {"label": "Booking requests per second, per event", "scale": "log", "min": 10, "max": 1000,
              "ticks": [10, 20, 50, 100, 200, 500, 1000]},
        "y": {"label": "% overbooked", "title": "Events overbooked (%)", "min": 0, "max": 100,
              "ticks": [0, 25, 50, 75, 100], "unit": "%"},
        "series": [
            {"name": "Plain read, then the lock", "short": "Read, then lock", "cls": "s1", "color": RED,
             "points": [[r, pct(share("mysql", "plainReadFirst", r))] for r in RATES]},
            {"name": "Every fix, and PostgreSQL at its default", "short": "Every fix", "cls": "s3",
             "points": [[r, pct(fix_max(r))] for r in RATES]},
        ],
    }

    def seats(db, v, c):
        counts = sold[(db, v, c)]
        return st.median(list(counts.elements()))

    flash = {
        "title": "A flash sale: everyone waiting on the lock gets a seat",
        "sub": ["10 seats. Each booker keeps trying until the event says it's full. 600 events per point (200 per round, 3 rounds).",
                "Median seats sold per event."],
        "x": {"label": "Bookers trying at once", "scale": "linear", "min": 0, "max": 50, "ticks": [0, 10, 20, 30, 40, 50]},
        "y": {"label": "seats", "title": "Seats sold for a 10-seat event", "min": 0, "max": 60,
              "ticks": [0, 10, 20, 30, 40, 50, 60], "unit": ""},
        "series": [
            {"name": "Plain read, then the lock", "short": "Read, then lock", "cls": "s1", "color": RED,
             "points": [[c, seats("mysql", "plainReadFirst", c)] for c in CONC]},
            {"name": "Every fix, and PostgreSQL at its default", "short": "Every fix", "cls": "s3",
             "points": [[c, max(seats(db, v, c) for db, v in FIXES)] for c in CONC]},
        ],
    }

    def cell(db, v, rate):
        k, n = over[(db, v, rate)]
        lo, hi = wilson(k, n)
        return f"{pct(k / n):g}% ({pct(lo):g}–{pct(hi):g})"

    rate_table = {
        "caption": "Share of events overbooked, with Wilson 95% intervals in brackets, 300 events per cell. Each fix, and "
                   "PostgreSQL at READ COMMITTED, had 0 of 300 at every rate, which puts its true rate below 1.3%.",
        "headers": ["Requests/s per event", "Plain read, then the lock", "Check in the lock's subquery",
                    "Every fix, PostgreSQL default"],
        "rows": [[f"{r:,}", cell("mysql", "plainReadFirst", r), cell("mysql", "subquery", r),
                  f"{sum(over[(db, v, r)][0] for db, v in FIXES)} of {sum(over[(db, v, r)][1] for db, v in FIXES):,}"]
                 for r in RATES],
    }

    checks = json.load(open(os.path.join(HERE, "checks.json")))
    res = {(c["db"], c["isolation"].lower().replace("-", " "), c["variant"]): c for c in checks}

    def verdict(db, iso, v):
        c = res.get((db, iso, v))
        if c is None:
            return "–"
        if c["t2"].startswith("error"):
            return "serialization error"
        return "overbooked" if c["overbooked"] else "safe"

    shapes = [
        ("Plain read, then the lock (the bug)", "plainReadFirst"),
        ("Lock first", "lockFirst"),
        ("Per-transaction READ COMMITTED", "readCommitted"),
        ("Counter on the locked row", "counter"),
        ("Capacity check in the lock's subquery, after a plain read", "subquery"),
        ("The same subquery as the first read", "subqueryFirst"),
        ("existsById, then the lock", "existsFirst"),
        ("getReferenceById, then the lock", "referenceFirst"),
        ("Writer locks first, inside a caller's transaction that already read", "outerRead"),
        ("The same writer with REQUIRES_NEW", "outerReadNewTransaction"),
        ("Counter fix, event loaded earlier in the transaction", "counterEventLoadedFirst"),
        ("The same, re-read with refresh(event, PESSIMISTIC_WRITE)", "counterEventLoadedThenRefreshed"),
    ]
    check_table = {
        "caption": "Two transactions on a 1-seat event: the first books and pauses before its commit, the second runs up to "
                   "its lock and waits for it. \"Overbooked\" means both got the seat.",
        "headers": ["Writer", "MySQL 8.4, REPEATABLE READ", "PostgreSQL 17, READ COMMITTED", "PostgreSQL 17, REPEATABLE READ"],
        "rows": [[name, verdict("mysql", "repeatable read", v), verdict("postgresql", "read committed", v),
                  verdict("postgresql", "repeatable read", v)] for name, v in shapes],
    }
    bug = arrivals["series"][0]["points"]
    aria = {
        "arrivals": "With a plain read before the lock, the share of overbooked events rose from "
                    f"{bug[0][1]:g}% at 10 booking requests per second to {bug[-1][1]:g}% at 1,000. "
                    "Locking first, READ COMMITTED, a counter on the locked row and PostgreSQL at its default isolation "
                    "never overbooked an event.",
        "flash": "With a plain read before the lock, a 10-seat event sold " +
                 ", ".join(f"{p[1]:g} seats with {p[0]} bookers" for p in flash["series"][0]["points"]) +
                 ". Every fix sold exactly 10.",
    }
    out = {"generated_by": "benchmarks/mysql-lock/make_charts.py", "arrivals": arrivals, "flash": flash,
           "rates": rate_table, "checks": check_table, "aria": aria,
           "notes": {"mean_request_ms": {f"{db}/{v}": round(st.median(x), 1) for (db, v), x in req_ms.items()}}}
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    json.dump(out, open(OUT, "w"), indent=1, ensure_ascii=False)
    for t in (rate_table, check_table):
        for r in t["rows"]:
            print(" | ".join(r))
    print(json.dumps(out["notes"]))
    print(flash["series"][0]["points"])


if __name__ == "__main__":
    main()
