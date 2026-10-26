#!/usr/bin/env python3
"""Generates content/posts/appcds/charts.json: every number the AppCDS post's charts and table show.

Reuses the raw results and the method of benchmarks/default-cds (same sessions, AppCDS was one of the
interleaved variants): paired by round, Hodges-Lehmann shift, bootstrap 95% CI of that shift, exact
Wilcoxon p, fixed seed, every displayed number rounded half away from zero and shipped as a string.
The comparison is AppCDS vs default CDS (the state after removing -Xshare:off).

Usage: python3 make_charts.py            (stdlib only)
"""
import json, math, os, random, statistics as st, sys

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "..", "default-cds")
sys.path.insert(0, DATA)
from paired_stats import hodges_lehmann, wilcoxon_p  # noqa: E402
from make_charts import rnd, num, boot_hl, paired  # noqa: E402  (post 1's helpers)

OUT = os.path.join(HERE, "..", "..", "content", "posts", "appcds", "charts.json")
SEED = 20261027

SETUPS = [  # label, results file, records per round, variant prefix
    ("ZGC", "results_gc.json", 12, "zgc"),
    ("G1", "results_gc.json", 12, "g1"),
    ("Parallel", "results_gc.json", 12, "parallel"),
    ("Serial", "results_gc.json", 12, "serial"),
    ("Heavy app (ZGC)", "results_pg.json", 3, "pg"),
]


def pair(recs, block, a, b, metric):
    """Paired values of variant a and b, matched by round."""
    va, vb = {}, {}
    for i, r in enumerate(recs):
        if r["variant"] == a:
            va[i // block] = r[metric]
        elif r["variant"] == b:
            vb[i // block] = r[metric]
    keys = sorted(va)
    return [va[k] for k in keys], [vb[k] for k in keys]


def main():
    rng = random.Random(SEED)
    rows, pooled, table = [], [], []
    for label, fname, block, p in SETUPS:
        recs = json.load(open(os.path.join(DATA, fname)))
        base, app = pair(recs, block, f"{p}_cds_default", f"{p}_appcds", "process")
        off, _ = pair(recs, block, f"{p}_xshare_off", f"{p}_appcds", "process")
        diffs = [rnd((b - a) * 1000, 3) for a, b in zip(base, app)]
        hl = hodges_lehmann(diffs)
        lo, hi = boot_hl(diffs, rng)
        pooled += [d / (st.median(base) * 1000) for d in diffs]

        mem = {}
        for m in ("rss", "rss_anon", "rss_file", "metaspace"):
            a, b = pair(recs, block, f"{p}_cds_default", f"{p}_appcds", m)
            d = [rnd(y - x, 6) for x, y in zip(a, b)]
            mlo, mhi = boot_hl(d, rng)
            mem[m] = {"base": st.median(a), "app": st.median(b), "shift": hodges_lehmann(d),
                      "ci": f"{num(mlo, 1, True)} to {num(mhi, 1, True)}", "lower": sum(x < 0 for x in d)}

        rel = -hl / (st.median(base) * 1000) * 100
        rows.append({
            "setup": label,
            "diffs": [{"v": rnd(d, 1), "label": num(d, signed=True)} for d in diffs],
            "shift": {"v": rnd(hl, 1), "label": num(hl, signed=True)},
            "ci": {"lo": rnd(lo, 1), "hi": rnd(hi, 1), "label": f"{num(lo, signed=True)} to {num(hi, signed=True)}"},
            "faster": sum(d < 0 for d in diffs), "n": len(diffs),
            "p": num(wilcoxon_p(diffs, rng), 3),
            "rel_label": num(rel, 0) + "%",
            "memory": {k: {"base": num(v["base"], 1), "app": num(v["app"], 1), "shift": num(v["shift"], 1, True),
                           "ci": v["ci"], "lower": v["lower"]}
                       for k, v in mem.items()},
        })
        # No -Xshare:off column: medians of separate boots can't be compared with each other (post 1).
        table.append([label, num(st.median(base), 2) + " s", num(st.median(app), 2) + " s",
                      f"{num(hl, signed=True)} ms ({num(lo, signed=True)} to {num(hi, signed=True)})",
                      f"{sum(d < 0 for d in diffs)}/{len(diffs)}"])

    plo, phi = boot_hl(pooled, rng)
    phl = hodges_lehmann(pooled)
    shifts = [r["shift"]["v"] for r in rows]

    cov = json.load(open(os.path.join(HERE, "coverage.json")))["apps"]
    coverage = []
    for app, name in (("small", "Small app"), ("heavy", "Heavy app")):
        for v, vname in (("default", "default CDS"), ("appcds", "AppCDS")):
            c = cov[app][v]
            coverage.append({"app": f"{name}, {vname}", "shared": c["shared"], "loaded": c["loaded"],
                             "shared_label": num(c["shared"]), "loaded_label": num(c["loaded"]),
                             "pct_label": num(100 * c["shared"] / c["loaded"], 1) + "%"})

    cost = json.load(open(os.path.join(HERE, "build_cost.json")))
    trap = json.load(open(os.path.join(HERE, "trap.json")))["cases"]

    small = [r for r in rows if r["setup"] != "Heavy app (ZGC)"]
    heavy = next(r for r in rows if r["setup"] == "Heavy app (ZGC)")
    data = {
        "generated_by": "benchmarks/appcds/make_charts.py",
        "seed": SEED,
        "titles": {
            "paired-shift": {"title": "Startup change with AppCDS, vs. default CDS",
                             "sub": ["Each dot is one round. Left of 0 is faster.",
                                     "Bar: 95% CI · tick: Hodges-Lehmann estimate"]},
            "class-coverage": {"title": "Classes loaded from a CDS archive at startup",
                               "sub": "Blue: from the archive · gray: loaded and parsed at startup"},
        },
        # Axis from the data: every round fits (no clipped points), zero stays visible on the right.
        "axis": {"paired-shift": {"min": 200 * math.floor(min(d["v"] for r in rows for d in r["diffs"]) / 200),
                                  "max": 200, "step": 200}},
        "pooled": {"faster": sum(r["faster"] for r in rows), "n": sum(r["n"] for r in rows),
                   "shift_label": num(-100 * phl, 1) + "%",
                   "ci_label": f"{num(-100 * phi, 1)}% to {num(-100 * plo, 1)}%"},
        # Saving per extra archived class (ZGC on both apps: the same GC as the coverage counts).
        "per_class_us": {
            "small_zgc": num(-rows[0]["shift"]["v"] * 1000 / (cov["small"]["appcds"]["shared"] - cov["small"]["default"]["shared"]), 1),
            "heavy_zgc": num(-heavy["shift"]["v"] * 1000 / (cov["heavy"]["appcds"]["shared"] - cov["heavy"]["default"]["shared"]), 1),
            "extra_classes_small": num(cov["small"]["appcds"]["shared"] - cov["small"]["default"]["shared"]),
            "extra_classes_heavy": num(cov["heavy"]["appcds"]["shared"] - cov["heavy"]["default"]["shared"]),
        },
        "summary": {
            "small_ms_range": f"{num(-max(r['shift']['v'] for r in small))} to {num(-min(r['shift']['v'] for r in small))} ms",
            "small_pct_range": f"{min(r['rel_label'] for r in small)} to {max(r['rel_label'] for r in small)}",
            "heavy_ms": num(-heavy["shift"]["v"]) + " ms", "heavy_pct": heavy["rel_label"],
        },
        "rows": rows,
        "table": {
            "headers": ["Setup", "Startup, default CDS (median)", "Startup, AppCDS (median)", "AppCDS vs default, paired (95% CI)", "Faster with AppCDS"],
            "rows": table,
        },
        "coverage": coverage,
        "build_cost": {
            "small_training_s": num(cost["small_app"]["training_s"], 1), "small_boot_s": num(cost["small_app"]["boot_only_s"], 1),
            "heavy_training_s": num(cost["heavy_app"]["training_s"], 1),
            "small_archive_mb": num(cost["small_app"]["archive_bytes"] / 1e6, 0),
            "heavy_archive_mb": num(cost["heavy_app"]["archive_bytes"] / 1e6, 0),
        },
        "trap": {k: {"shared": num(v["shared_classes"]), "loaded": num(v["loaded_classes"]),
                     "xshare_on_exit": v["xshare_on_exit_code"], "version_sharing": v["java_version_says_sharing"],
                     "messages": len(v["jvm_messages_without_xlog"])} for k, v in trap.items()},
        "aria": {
            "paired-shift": (f"AppCDS made startup faster than default CDS in all {sum(r['faster'] for r in rows)} of "
                             f"{sum(r['n'] for r in rows)} paired boots, by about {num(-max(shifts))} to {num(-min(shifts))} ms."),
            "class-coverage": ("With default CDS about 1,400 classes come from the archive; with AppCDS "
                               f"{coverage[1]['shared_label']} of {coverage[1]['loaded_label']} on the small app and "
                               f"{coverage[3]['shared_label']} of {coverage[3]['loaded_label']} on the heavy app."),
        },
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w") as f:
        json.dump(data, f, indent=1, ensure_ascii=False)
        f.write("\n")
    print(f"wrote {os.path.relpath(OUT, HERE)}")
    print(f"pooled: {data['pooled']['shift_label']} faster, CI {data['pooled']['ci_label']}, {data['pooled']['faster']}/{data['pooled']['n']}")
    for r in rows:
        print(f"  {r['setup']:16} {r['shift']['label']:>7} ms ({r['rel_label']})  CI {r['ci']['label']:>18}  {r['faster']}/10  p={r['p']}  "
              f"anon {r['memory']['rss_anon']['base']}->{r['memory']['rss_anon']['app']} ({r['memory']['rss_anon']['shift']})  "
              f"rss {r['memory']['rss']['shift']} ({r['memory']['rss']['ci']}, lower {r['memory']['rss']['lower']}/10)  file {r['memory']['rss_file']['shift']}  metaspace {r['memory']['metaspace']['base']}->{r['memory']['metaspace']['app']}")
    for row in table:
        print("  table:", row)
    print("  summary:", data["summary"])
    print("  build:", data["build_cost"])


if __name__ == "__main__":
    main()
