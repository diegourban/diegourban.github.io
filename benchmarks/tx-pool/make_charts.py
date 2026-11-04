#!/usr/bin/env python3
"""Chart and table data for the transaction post: content/posts/transactional-pool/charts.json.

- "breaks": eager's first failing step by partner share, against the pool capacity from the measured hold times.
- "failures": share of requests that failed at each arrival rate (median of the rounds), eager at 100/50/25%
  partner orders, plus lazy and narrowed, which never failed at any share.
- "latency": the own-stock orders' p95 at 50% partner orders, eager vs lazy vs narrowed (median of the rounds).
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


SHARES = [1.0, 0.95, 0.9, 0.75, 0.5, 0.25]


def main():
    rows, by = load()
    analysis = json.load(open(os.path.join(HERE, "analysis.json")))
    bp = breakpoints(rows)
    med_bp = {sh: st.median(bp[("eager", sh)]) for sh in SHARES}
    pct = lambda r: 100 * r["failed_rate"]
    calm = lambda x: max(p[1] for s in ("lazy", "narrowed") for sh in SHARES for p in series(by, s, sh, pct) if p[0] == x)
    model = analysis["capacity_model"]
    hold = analysis["hold_ms"]
    capacity = lambda s: 10 / (s * hold["downstream_median"] / 1000 + (1 - s) * hold["db_median_at_100_plus"] / 1000)
    breaks = {
        "title": "Where the pool runs out",
        "sub": ["First failing step with eager fetching, by the share of orders sent to the partner. Same in all 3 rounds.",
                f"Capacity: 10 connections / ({hold['downstream_median']:.0f} ms per partner order and "
                f"{hold['db_median_at_100_plus']:.1f} ms per own-stock order, weighted by the share)."],
        "x": {"label": "Orders sent to the partner (%)", "scale": "linear", "min": 25, "max": 100,
              "ticks": [25, 50, 75, 90, 100]},
        "y": {"label": "req/s", "min": 0, "max": 300, "ticks": [0, 50, 100, 150, 200, 250, 300], "unit": "req/s"},
        "series": [
            {"name": "Eager: first failing step", "short": "Eager", "cls": "s1",
             "points": [[round(100 * sh), med_bp[sh]] for sh in sorted(SHARES)]},
            {"name": "Pool capacity from the measured hold times", "short": "Capacity", "cls": "s2", "dash": True,
             "points": [[x, round(capacity(x / 100), 1)] for x in range(25, 101, 5)]},
            {"name": "Lazy or narrowed: no failures up to 300 (the highest rate tested)", "short": "Lazy/narrowed",
             "cls": "rest", "points": [[25, 300], [100, 300]]},
        ],
    }
    failures = {
        "title": "Failed requests as the arrival rate rises",
        "sub": ["Pool of 10, 2 s timeout, 300 ms partner call. Median of 3 rounds.",
                "Each run stopped after three failing steps in a row."],
        "x": X, "y": {"label": "% failed", "min": 0, "max": 50, "ticks": [0, 10, 20, 30, 40, 50], "unit": "%"},
        "series": [
            {"name": "Eager, 100% partner orders", "short": "Eager 100%", "cls": "s1", "points": series(by, "eager", 1.0, pct)},
            {"name": "Eager, 50% partner orders", "short": "Eager 50%", "cls": "s2", "points": series(by, "eager", 0.5, pct)},
            {"name": "Eager, 25% partner orders", "short": "Eager 25%", "cls": "s3", "points": series(by, "eager", 0.25, pct)},
            {"name": "Lazy or narrowed, any share", "short": "Lazy/narrowed", "cls": "rest",
             "points": [[x, calm(x)] for x in RATES]},
        ],
    }
    p95 = lambda r: r["db"]["p(95)"]
    lat = {s: series(by, s, 0.5, p95) for s in ("eager", "lazy", "narrowed")}
    latency = {
        "title": "Own-stock orders wait for a connection",
        "sub": ["p95 of the own-stock orders, with half the orders sent to the partner. Median of 3 rounds.",
                f"Eager leaves the chart at {med_bp[0.5]:.0f} req/s: from there it is the 2 s pool timeout."],
        "x": X, "y": {"label": "p95 (ms)", "min": 0, "max": 200, "ticks": [0, 50, 100, 150, 200], "unit": "ms"},
        "series": [
            {"name": "Eager (default)", "short": "Eager", "cls": "s1", "points": lat["eager"]},
            {"name": "Lazy (connection-fetch=lazy)", "short": "Lazy", "cls": "s2", "points": lat["lazy"]},
            {"name": "Narrowed transaction", "short": "Narrowed", "cls": "s3", "dash": True, "points": lat["narrowed"]},
        ],
    }
    label = {"eager": "Eager (default)", "lazy": "Lazy (connection-fetch=lazy)", "narrowed": "Narrowed transaction"}
    fmt = lambda v: "none" if v is None else f"{v}"
    table = {"caption": "The first rate at which more than 1% of orders failed, the same step in all three rounds. "
                        "\"none\": no step failed up to 300 req/s, the highest rate tested.",
             "headers": ["First failing step, req/s (tested up to 300)"] + [f"{round(100 * sh)}% partner" for sh in SHARES],
             "rows": [[label[s]] + [", ".join(sorted({fmt(v) for v in bp[(s, sh)]})) for sh in SHARES]
                      for s in ["eager", "lazy", "narrowed"]]}
    calm_lat = max(p[1] for s in ("lazy", "narrowed") for p in lat[s])
    pre = {x: y for x, y in lat["eager"]}
    aria = {
        "breaks": "With eager connection fetching, the first failing step was " +
                  ", ".join(f"{med_bp[sh]:.0f} req/s at {round(100 * sh)}%" for sh in SHARES) +
                  " of orders sent to the partner, each just above the pool capacity the hold times predict. "
                  "Lazy fetching and a narrowed transaction never failed up to 300 req/s.",
        "failures": f"With eager fetching, requests started failing at {med_bp[1.0]:.0f} req/s when every order went to the "
                    f"partner, at {med_bp[0.5]:.0f} with half, and at {med_bp[0.25]:.0f} with a quarter. "
                    "With lazy fetching or a narrowed transaction, no request failed at any rate or share.",
        "latency": f"With half the orders sent to the partner, the own-stock orders' p95 stayed at or below "
                   f"{calm_lat:.0f} ms for lazy and narrowed at every rate. With eager fetching it was {pre[40]:.0f} ms at "
                   f"40 req/s, {pre[60]:.0f} ms at 60 and about 2,000 ms, the pool timeout, at {med_bp[0.5]:.0f}.",
    }
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

    check_table = {"caption": "Pool connections checked out during a 300 ms downstream call, on an otherwise idle app, "
                              "with eager or lazy connection fetching and open session in view (OSIV) off or on. Three "
                              "repeats each, and every repeat agreed.",
                   "headers": ["Method (connections held during the call)", "Eager", "Lazy", "Eager + OSIV", "Lazy + OSIV"],
                   "rows": [[name] + [held(k, n) for k in ["eager", "lazy", "eager_osiv", "lazy_osiv"]] for name, n in shapes]}
    out = {"generated_by": "benchmarks/tx-pool/make_charts.py", "breaks": breaks, "failures": failures,
           "latency": latency, "table": table, "checks": check_table, "aria": aria}
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    json.dump(out, open(OUT, "w"), indent=1, ensure_ascii=False)
    for r in table["rows"]:
        print(" | ".join(r))
    for r in check_table["rows"]:
        print(" | ".join(r))


if __name__ == "__main__":
    main()
