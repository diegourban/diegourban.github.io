#!/usr/bin/env python3
"""Chart and table data for the transaction post: content/posts/transactional-pool/charts.json.

- "failures": share of requests that failed at each arrival rate (median of the rounds), eager at 90/50/10%
  downstream, plus lazy and narrowed, which never failed at any share.
- "latency": the database path's p95 at 50% downstream, eager vs lazy vs narrowed (median of the rounds).
- "table": the breakpoint per setup and share (first rate with more than 1% failed, per round).
- "checks": pool connections held during the slow call, from checks.json.
Every number in the post comes from here or from results.jsonl directly.
"""
import collections, json, os, statistics as st

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "..", "content", "posts", "transactional-pool", "charts.json")
FAIL = 0.01
RATES = [10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 125, 150, 200, 250, 300]
X = {"label": "Requests per second", "scale": "linear", "min": 0, "max": 300, "ticks": [0, 50, 100, 150, 200, 250, 300]}


def load():
    rows = [json.loads(l) for l in open(os.path.join(HERE, "results.jsonl"))]
    by = collections.defaultdict(list)
    for r in rows:
        by[(r["setup"], r["share"], r["rate"])].append(r)
    return rows, by


def series(by, setup, share, value):
    pts = []
    for rate in RATES:
        rs = by.get((setup, share, rate))
        if not rs:
            break
        pts.append([rate, round(st.median(value(r) for r in rs), 1)])
    return pts


def breakpoints(rows):
    steps = collections.defaultdict(list)
    for r in rows:
        steps[(r["setup"], r["share"], r["round"])].append(r)
    out = collections.defaultdict(list)
    for (setup, share, rnd), rs in sorted(steps.items()):
        rs.sort(key=lambda r: r["rate"])
        out[(setup, share)].append(next((r["rate"] for r in rs if r["failed_rate"] > FAIL), None))
    return out


def main():
    rows, by = load()
    pct = lambda r: 100 * r["failed_rate"]
    failures = {
        "title": "Failed requests as the arrival rate rises",
        "sub": ["Pool of 10, 2 s timeout, 300 ms downstream call. Median of 3 rounds.",
                "Each run stopped after three failing steps in a row."],
        "x": X, "y": {"label": "% failed", "min": 0, "max": 40, "ticks": [0, 10, 20, 30, 40], "unit": "%"},
        "series": [
            {"name": "Eager, 90% downstream", "short": "Eager 90%", "cls": "s1", "points": series(by, "eager", 0.9, pct)},
            {"name": "Eager, 50% downstream", "short": "Eager 50%", "cls": "s2", "points": series(by, "eager", 0.5, pct)},
            {"name": "Eager, 10% downstream", "short": "Eager 10%", "cls": "s3", "points": series(by, "eager", 0.1, pct)},
            {"name": "Lazy or narrowed, any share", "short": "Lazy/narrowed", "cls": "rest",
             "points": [[x, max(p[1] for s in ("lazy", "narrowed") for sh in (0.1, 0.5, 0.9)
                                for p in series(by, s, sh, pct) if p[0] == x)] for x in RATES]},
        ],
    }
    p95 = lambda r: r["db"]["p(95)"]
    latency = {
        "title": "Database requests wait for a connection",
        "sub": ["p95 of the database-path requests, 50% downstream. Median of 3 rounds.",
                "Eager leaves the chart at 70 req/s: from there it is the 2 s pool timeout."],
        "x": X, "y": {"label": "p95 (ms)", "min": 0, "max": 80, "ticks": [0, 20, 40, 60, 80], "unit": "ms"},
        "series": [
            {"name": "Eager (default)", "short": "Eager", "cls": "s1", "points": series(by, "eager", 0.5, p95)},
            {"name": "Lazy (connection-fetch=lazy)", "short": "Lazy", "cls": "s2", "points": series(by, "lazy", 0.5, p95)},
            {"name": "Narrowed transaction", "short": "Narrowed", "cls": "s3", "dash": True, "points": series(by, "narrowed", 0.5, p95)},
        ],
    }
    bp = breakpoints(rows)
    label = {"eager": "Eager (default)", "lazy": "Lazy (connection-fetch=lazy)", "narrowed": "Narrowed transaction"}
    fmt = lambda v: "none" if v is None else f"{v} req/s"
    table = {"headers": ["First failing step (tested up to 300 req/s)", "90% downstream", "50% downstream", "10% downstream"],
             "rows": [[label[s]] + [", ".join(sorted({fmt(v) for v in bp[(s, sh)]})) for sh in [0.9, 0.5, 0.1]]
                      for s in ["eager", "lazy", "narrowed"]]}
    checks = json.load(open(os.path.join(HERE, "checks.json")))
    shapes = [  # in the order of the post's three cases, then the surprises
        ("No query: @Transactional", "required"),
        ("No query: @Transactional(readOnly = true)", "readOnly"),
        ("No query: same, with the JDBC transaction manager", ["jdbcRequired", "jdbcReadOnly"]),
        ("No query: NOT_SUPPORTED, NEVER, SUPPORTS or no annotation", ["notSupported", "never", "supports", "none"]),
        ("@Transactional, the call, then a query", "callThenQuery"),
        ("@Transactional, a query, then the call", "queryThenCall"),
        ("SUPPORTS or NOT_SUPPORTED, an EntityManager query, then the call",
         ["emSupportsQueryThenCall", "emNotSupportedQueryThenCall"]),
        ("No annotation, an EntityManager query, then the call", "emNoneQueryThenCall"),
        ("No transaction, a Spring Data repository call, then the call",
         ["supportsQueryThenCall", "notSupportedQueryThenCall", "noneQueryThenCall"]),
        ("Outer @Transactional queried, inner NOT_SUPPORTED makes the call", "suspended"),
        ("Outer @Transactional queried, inner REQUIRES_NEW queries and makes the call", "requiresNewInsideQueried"),
    ]

    def held(key, names):
        names = names if isinstance(names, list) else [names]
        vals = {v for n in names for v in checks[key][n]}
        assert len(vals) == 1, (key, names, vals)  # every repeat and every variant in a row agree
        return str(vals.pop())

    check_table = {"headers": ["Method (connections held during the call)", "Eager", "Lazy", "Eager + OSIV", "Lazy + OSIV"],
                   "rows": [[name] + [held(k, n) for k in ["eager", "lazy", "eager_osiv", "lazy_osiv"]] for name, n in shapes]}
    out = {"generated_by": "benchmarks/tx-pool/make_charts.py", "failures": failures, "latency": latency,
           "table": table, "checks": check_table,
           "aria": {"failures": "With eager connection fetching, requests started failing at 40 req/s with 90% of them calling "
                                "the slow service, and at 70 req/s with 50%. With 10% no step failed up to 300 req/s. "
                                "With lazy fetching or a narrowed transaction, no request failed at any rate or share.",
                    "latency": "At 50% downstream, the database requests' p95 stayed at or below about 16 ms for lazy and narrowed at "
                               "every rate. With eager fetching it rose to 64 ms at 60 req/s and to about 2,000 ms, the pool "
                               "timeout, at 70 req/s."}}
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    json.dump(out, open(OUT, "w"), indent=1, ensure_ascii=False)
    for r in table["rows"]:
        print(" | ".join(r))
    for r in check_table["rows"]:
        print(" | ".join(r))


if __name__ == "__main__":
    main()
