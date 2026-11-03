#!/usr/bin/env python3
"""Numbers behind the transaction post that aren't plotted: the capacity model from measured hold times, the
breakpoints and warnings per round, and paired comparisons of the database requests' p95. Writes analysis.json.

Pairing: each round runs every (setup, share) combination once, in random order, so runs are paired by
(share, round): 18 pairs, or 15 for the database requests, which don't exist at 100% partner orders. A run's value is the median over its steps of the per-step comparison. The shift is the
Hodges-Lehmann estimate, its 95% CI a bootstrap of that estimate (fixed seed), and p an exact Wilcoxon signed-rank
test, the same code as the earlier posts.
"""
import collections, json, os, random, statistics as st, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "default-cds"))
from paired_stats import hodges_lehmann, wilcoxon_p  # noqa: E402

SEED, BOOT = 20261027, 10000
SHARES = (1.0, 0.95, 0.9, 0.75, 0.5, 0.25)
DB_SHARES = tuple(s for s in SHARES if s < 1.0)
FAIL = 0.01
POOL = 10


def boot_hl(d, rng):
    m = sorted(hodges_lehmann(rng.choices(d, k=len(d))) for _ in range(BOOT))
    return m[int(.025 * BOOT)], m[int(.975 * BOOT)]


def paired(d, rng):
    lo, hi = boot_hl(d, rng)
    return {"n": len(d), "shift": hodges_lehmann(d), "ci": [lo, hi], "positive": sum(x > 0 for x in d),
            "p": wilcoxon_p(d, rng), "values": d}


def main():
    rows = [json.loads(l) for l in open(os.path.join(HERE, "results.jsonl"))]
    runs = collections.defaultdict(dict)  # (setup, share, round) -> rate -> row
    for r in rows:
        runs[(r["setup"], r["share"], r["round"])][r["rate"]] = r
    rng = random.Random(SEED)
    out = {}

    # Hold times: the downstream call as the eager runs saw it, and a database request once the JVM is warm.
    down = st.median(r["downstream"]["med"] for r in rows if r["setup"] == "eager" and r["downstream"]["n"])
    db = st.median(r["db"]["med"] for r in rows if r["setup"] == "lazy" and r["rate"] >= 100 and r["db"]["n"])
    out["hold_ms"] = {"downstream_median": down, "db_median_at_100_plus": db}
    out["capacity_model"] = {str(s): POOL / (s * down / 1000 + (1 - s) * db / 1000) for s in SHARES}

    # Per round: the breakpoint, the first step with a waiting thread, and what failed at the breakpoint.
    per_round = []
    for (setup, share, rnd), steps in sorted(runs.items()):
        if setup != "eager":
            continue
        rates = sorted(steps)
        bp = next((x for x in rates if steps[x]["failed_rate"] > FAIL), None)
        wait = next((x for x in rates if (steps[x]["pool_pending_max"] or 0) > 0), None)
        row = {"share": share, "round": rnd, "breakpoint": bp, "first_waiting": wait,
               "last_passing": max((x for x in rates if bp is None or x < bp), default=None)}
        if bp:
            s = steps[bp]
            row.update({"db_failed": s["db"]["failed_rate"], "downstream_failed": s["downstream"]["failed_rate"],
                        "failed": s["failed_rate"], "pending_max_at_breakpoint": s["pool_pending_max"],
                        "db_p95_at_breakpoint": s["db"]["p(95)"]})
        per_round.append(row)
    out["eager_per_round"] = per_round

    # Narrowed vs lazy: database p95 difference per step (ms), median per run.
    d = []
    for share in DB_SHARES:
        for rnd in (1, 2, 3):
            n, l = runs[("narrowed", share, rnd)], runs[("lazy", share, rnd)]
            d.append(st.median(n[x]["db"]["p(95)"] - l[x]["db"]["p(95)"] for x in sorted(set(n) & set(l))))
    out["narrowed_minus_lazy_db_p95_ms"] = paired(d, rng)

    # Lazy vs eager while the eager run showed no waiting thread: relative difference per step (%), median per run.
    d = []
    for share in DB_SHARES:
        for rnd in (1, 2, 3):
            e, l = runs[("eager", share, rnd)], runs[("lazy", share, rnd)]
            quiet = [x for x in sorted(set(e) & set(l)) if (e[x]["pool_pending_max"] or 0) == 0]
            d.append(st.median(100 * (l[x]["db"]["p(95)"] / e[x]["db"]["p(95)"] - 1) for x in quiet))
    out["lazy_vs_eager_db_p95_pct_no_waiting"] = paired(d, rng)

    json.dump(out, open(os.path.join(HERE, "analysis.json"), "w"), indent=1)
    print(json.dumps({k: v for k, v in out.items() if k != "eager_per_round"}, indent=1, default=str)[:2500])
    for r in per_round:
        print(r)


if __name__ == "__main__":
    main()
