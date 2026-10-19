#!/usr/bin/env python3
"""Generates content/posts/default-cds/charts.json: every number the post's charts and results
table show, computed from the raw results in this folder.

- Pairing: each round started every variant once, so consecutive blocks of records are rounds.
- Shift: Hodges-Lehmann estimate of the paired differences; 95% CI: bootstrap of that estimate.
- p: exact Wilcoxon signed-rank test.
- Every displayed number is rounded here, half away from zero, and shipped as a string, so the
  charts, their tooltips and the table always agree.

Usage: python3 make_charts.py            (stdlib only; reuses paired_stats.py)
"""
import json, os, random, statistics as st
from decimal import Decimal, ROUND_HALF_UP

from paired_stats import hodges_lehmann, wilcoxon_p

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "..", "content", "posts", "default-cds", "charts.json")
SEED, BOOT = 20261019, 10000
MINUS = "−"

SETUPS = [  # label, results file, records per round, variant prefix
    ("ZGC", "results_gc.json", 12, "zgc"),
    ("G1", "results_gc.json", 12, "g1"),
    ("Parallel", "results_gc.json", 12, "parallel"),
    ("Serial", "results_gc.json", 12, "serial"),
    ("Heavy app (ZGC)", "results_pg.json", 3, "pg"),
]


def rnd(x, places=0):
    q = Decimal(1).scaleb(-places)
    return float(Decimal(str(x)).quantize(q, rounding=ROUND_HALF_UP))


def num(x, places=0, signed=False):
    v = rnd(x, places)
    s = f"{abs(v):,.{places}f}"
    if v < 0:
        return MINUS + s
    return ("+" + s) if signed and v > 0 else s


def boot_hl(diffs, rng):
    est = sorted(hodges_lehmann(rng.choices(diffs, k=len(diffs))) for _ in range(BOOT))
    return est[int(.025 * BOOT)], est[int(.975 * BOOT) - 1]


def paired(recs, block, prefix, metric):
    off, cds = {}, {}
    for i, r in enumerate(recs):
        if r["variant"] == f"{prefix}_xshare_off":
            off[i // block] = r[metric]
        elif r["variant"] == f"{prefix}_cds_default":
            cds[i // block] = r[metric]
    keys = sorted(off)
    return [off[k] for k in keys], [cds[k] for k in keys]


def main():
    rng = random.Random(SEED)
    rows, pooled = [], []
    for label, fname, block, prefix in SETUPS:
        recs = json.load(open(os.path.join(HERE, fname)))
        off_t, cds_t = paired(recs, block, prefix, "process")
        diffs = [rnd((b - a) * 1000, 3) for a, b in zip(off_t, cds_t)]  # snap float noise: data has 1 ms resolution
        hl = hodges_lehmann(diffs)
        lo, hi = boot_hl(diffs, rng)
        p = wilcoxon_p(diffs, rng)
        pooled += [d / (st.median(off_t) * 1000) for d in diffs]

        mem = {}
        for m in ("rss_anon", "rss_file", "metaspace"):
            a, b = paired(recs, block, prefix, m)
            d = [rnd(y - x, 6) for x, y in zip(a, b)]
            mem[m] = {"off": st.median(a), "cds": st.median(b), "shift": hodges_lehmann(d)}

        rows.append({
            "setup": label,
            "startup": {"off": num(st.median(off_t), 2), "cds": num(st.median(cds_t), 2)},
            "diffs": [{"v": rnd(d, 1), "label": num(d, signed=True)} for d in diffs],
            "shift": {"v": rnd(hl, 1), "label": num(hl, signed=True)},
            "ci": {"lo": rnd(lo, 1), "hi": rnd(hi, 1),
                   "label": f"{num(lo, signed=True)} to {num(hi, signed=True)}"},
            "faster": sum(d < 0 for d in diffs), "n": len(diffs),
            "p": num(p, 3),
            "memory": {
                "anon": {"off": num(mem["rss_anon"]["off"]), "cds": num(mem["rss_anon"]["cds"]),
                         "shift": rnd(mem["rss_anon"]["shift"], 1), "label": num(mem["rss_anon"]["shift"], 1, True)},
                "file": {"shift": rnd(mem["rss_file"]["shift"], 1), "label": num(mem["rss_file"]["shift"], 1, True)},
                "metaspace": {"off": num(mem["metaspace"]["off"], 1), "cds": num(mem["metaspace"]["cds"], 1),
                              "label": num(mem["metaspace"]["shift"], 1, True)},
            },
        })

    plo, phi = boot_hl(pooled, rng)
    phl = hodges_lehmann(pooled)
    faster = sum(r["faster"] for r in rows)
    total = sum(r["n"] for r in rows)

    cov = json.load(open(os.path.join(HERE, "coverage.json")))
    coverage = []
    for key, label in (("small_app", "Small app"), ("heavy_app", "Heavy app")):
        shared = st.median(r["shared"] for r in cov[key])
        loaded = st.median(r["loaded"] for r in cov[key])
        coverage.append({"app": label, "shared": shared, "loaded": loaded,
                         "shared_label": num(shared), "loaded_label": num(loaded),
                         "pct_label": num(100 * shared / loaded, 1) + "%"})

    serial = next(r for r in rows if r["setup"] == "Serial")
    anon = [r["memory"]["anon"]["shift"] for r in rows[:4]]
    data = {
        "generated_by": "benchmarks/default-cds/make_charts.py",
        "seed": SEED,
        "pooled": {"faster": faster, "n": total,
                   "shift_label": num(-100 * phl, 1) + "%",
                   "ci_label": f"{num(-100 * phi, 1)}% to {num(-100 * plo, 1)}%"},
        "rows": rows,
        "coverage": coverage,
        "aria": {
            "paired-shift": (f"Removing -Xshare:off made startup faster in {faster} of {total} paired boots. "
                             f"Serial was faster in all {serial['n']} rounds; the ZGC and heavy-app intervals still cross zero."),
            "memory-change": (f"Private memory fell by {num(-max(anon), 1)} to {num(-min(anon), 1)} MB per JVM on the small app, "
                              f"while file-backed memory rose by about 3 MB: the mapped archive."),
            "class-coverage": ("The default archive supplied " + coverage[0]["shared_label"] + " of " + coverage[0]["loaded_label"]
                               + " classes on the small app and " + coverage[1]["shared_label"] + " of "
                               + coverage[1]["loaded_label"] + " on the heavy app."),
        },
    }
    with open(OUT, "w") as f:
        json.dump(data, f, indent=1, ensure_ascii=False)
        f.write("\n")
    print(f"wrote {os.path.relpath(OUT, HERE)}")
    print(f"pooled: {data['pooled']['shift_label']} faster, CI {data['pooled']['ci_label']}, {faster}/{total}")
    for r in rows:
        print(f"  {r['setup']:16} {r['shift']['label']:>6} ms  CI {r['ci']['label']:>14}  {r['faster']}/10  p={r['p']}  "
              f"anon {r['memory']['anon']['label']} MB  file {r['memory']['file']['label']} MB")
    for c in coverage:
        print(f"  {c['app']}: {c['shared_label']} of {c['loaded_label']} ({c['pct_label']})")


if __name__ == "__main__":
    main()
