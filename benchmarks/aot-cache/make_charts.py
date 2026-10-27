#!/usr/bin/env python3
"""Every number in post 3's charts and table, from results.jsonl, analysis.json and training.json.

  python3 make_charts.py      # writes content/posts/aot-cache/charts.json

Paired shifts are recomputed here per round (same seed and method as analyze.py) so each chart dot is one round.
Values are rounded half away from zero and pre-formatted, so the charts never format numbers themselves.
"""
import json, os, random, statistics as st, sys
from decimal import Decimal, ROUND_HALF_UP

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "default-cds"))
from paired_stats import hodges_lehmann, wilcoxon_p  # noqa: E402

SEED, BOOT = 20261027, 10000
OUT = os.path.join(HERE, "..", "..", "content", "posts", "aot-cache", "charts.json")
GC_LABEL = {"g1": "G1", "parallel": "Parallel", "serial": "Serial", "zgc": "ZGC"}


def rnd(x, places=0):
    q = Decimal(1).scaleb(-places)
    return Decimal(repr(round(x, 9))).quantize(q, rounding=ROUND_HALF_UP)


def fmt(x, places=0, sign=False):
    d = rnd(x, places)
    s = f"{abs(d):,.{places}f}"
    if d < 0:
        return "−" + s
    return ("+" + s) if sign and d > 0 else s


def load():
    recs = [json.loads(l) for l in open(os.path.join(HERE, "results.jsonl"))]
    by = {}
    for r in recs:
        if r.get("failed"):
            continue
        by.setdefault(r["variant"], {})[r["round"]] = r
    return by


def warmup_total(r):
    return sum(r["latency_us"]) / 1000  # ms


def paired_row(by, a, b, value, label, rng):
    rounds = sorted(set(by[a]) & set(by[b]))
    d = [value(by[a][k]) - value(by[b][k]) for k in rounds]
    boots = sorted(hodges_lehmann(rng.choices(d, k=len(d))) for _ in range(BOOT))
    lo, hi = boots[int(.025 * BOOT)], boots[int(.975 * BOOT)]
    hl = hodges_lehmann(d)
    return {"setup": label, "diffs": [{"v": float(rnd(x)), "label": fmt(x, sign=True)} for x in d],
            "shift": {"v": float(rnd(hl)), "label": fmt(hl, sign=True)},
            "ci": {"lo": float(rnd(lo)), "hi": float(rnd(hi)), "label": f"{fmt(lo, sign=True)} to {fmt(hi, sign=True)}"},
            "faster": sum(x < 0 for x in d), "n": len(d), "p": f"{wilcoxon_p(d, rng):.3f}"}


def main():
    by = load()
    an = json.load(open(os.path.join(HERE, "analysis.json")))
    training = json.load(open(os.path.join(HERE, "training.json")))
    rng = random.Random(SEED)
    startup = lambda r: r["process"] * 1000

    start_rows = [paired_row(by, f"aot_{g}", f"appcds_{g}", startup, GC_LABEL[g] + (" (JDK 25)" if g == "zgc" else ""), rng)
                  for g in ("g1", "parallel", "serial", "zgc")]
    start_rows.append(paired_row(by, "heavy_aot_zgc", "heavy_appcds_zgc", startup, "Heavy app (ZGC)", rng))

    warm_rows = [paired_row(by, f"aot_{g}", f"appcds_{g}", warmup_total, GC_LABEL[g] + (" (JDK 25)" if g == "zgc" else ""), rng)
                 for g in ("g1", "parallel", "serial", "zgc")]
    warm_rows.append(paired_row(by, "heavy_aot_zgc", "heavy_appcds_zgc", warmup_total, "Heavy app (ZGC)", rng))

    train_rows = [paired_row(by, f"aot_g1_n{n}", "aot_g1", warmup_total, lab, rng)
                  for n, lab in ((0, "0 (up to onRefresh)"), (200, "200"), (1000, "1,000"), (20000, "20,000"))]

    jdk_rows = [paired_row(by, "t26_aot_zgc", "t25_aot_zgc", startup, "Startup", rng),
                paired_row(by, "t26_aot_zgc", "t25_aot_zgc", warmup_total, "2,000 requests", rng)]

    # Warm-up curves (G1): median over rounds of 20-request buckets; x = bucket middle.
    curves = an["curves_ms"]
    xs = [i * 20 + 10 for i in range(len(curves["aot_g1"]))]
    pts = lambda v: [[x, y] for x, y in zip(xs, curves[v])]
    warm_curve = {
        "title": "Latency during the first 2,000 requests (G1)",
        "sub": ["Median of 20-request blocks over 10 rounds. Lower is faster."],
        "x": {"label": "Request number", "scale": "linear", "min": 0, "max": 2000, "ticks": [0, 500, 1000, 1500, 2000]},
        "y": {"label": "ms", "min": 0, "max": 3, "ticks": [0, 1, 2, 3], "unit": "ms"},
        "series": [
            {"name": "Default CDS", "short": "Default CDS", "cls": "rest", "points": pts("cds_g1")},
            {"name": "AppCDS", "short": "AppCDS", "cls": "s1", "points": pts("appcds_g1")},
            {"name": "AOT cache, trained with requests", "short": "AOT, trained", "cls": "s2", "points": pts("aot_g1")},
            {"name": "AOT cache, trained up to onRefresh", "short": "AOT, onRefresh", "cls": "s3", "dash": True, "points": pts("aot_g1_n0")},
        ],
    }
    sig = an["signal"]
    signal_curve = {
        "title": "What the JVM was still doing while the cache was being trained",
        "sub": ["Per 100 requests of a 20,000-request training run (G1)."],
        "x": {"label": "Requests served", "scale": "log", "min": 100, "max": 20000, "ticks": [100, 1000, 10000]},
        "y": {"label": "per 100 requests", "min": 0, "max": 1500, "ticks": [0, 500, 1000, 1500], "unit": ""},
        "series": [
            {"name": "Methods JIT-compiled", "short": "JIT compilations", "cls": "s2", "points": [[s["requests"], s["compilations"]] for s in sig]},
            {"name": "New classes loaded", "short": "New classes", "cls": "s1", "points": [[s["requests"], s["classes"]] for s in sig]},
        ],
    }

    med = lambda v, m: st.median(r[m] for r in by[v].values())
    table_rows = []
    for g in ("g1", "parallel", "serial", "zgc"):
        sr = next(r for r in start_rows if r["setup"].startswith(GC_LABEL[g]))
        wr = next(r for r in warm_rows if r["setup"].startswith(GC_LABEL[g]))
        table_rows.append([sr["setup"], f"{fmt(med(f'cds_{g}', 'process'), 2)} s", f"{fmt(med(f'appcds_{g}', 'process'), 2)} s",
                           f"{fmt(med(f'aot_{g}', 'process'), 2)} s", f"{sr['shift']['label']} ms ({sr['ci']['label']})",
                           f"{wr['shift']['label']} ms ({wr['ci']['label']})"])
    hs, hw = start_rows[-1], warm_rows[-1]
    table_rows.append(["Heavy app (ZGC)", f"{fmt(med('heavy_cds_zgc', 'process'), 2)} s", f"{fmt(med('heavy_appcds_zgc', 'process'), 2)} s",
                       f"{fmt(med('heavy_aot_zgc', 'process'), 2)} s", f"{hs['shift']['label']} ms ({hs['ci']['label']})",
                       f"{hw['shift']['label']} ms ({hw['ci']['label']})"])

    sizes = {(t["image_set"], t["archive"]): t for t in training}
    mb = lambda s, a: fmt(sizes[(s, a)]["bytes"] / 2**20)
    out = {
        "generated_by": "benchmarks/aot-cache/make_charts.py", "seed": SEED,
        "startup": {"rows": start_rows, "axis": {"min": -400, "max": 200, "step": 100},
                    "titles": {"title": "Startup with the AOT cache, vs. AppCDS",
                               "sub": ["Each dot is one round. Left of 0 is faster.", "Bar: 95% CI · tick: Hodges-Lehmann estimate"]}},
        "training": {"rows": train_rows, "axis": {"min": -400, "max": 1600, "step": 400},
                     "titles": {"title": "Extra time for 2,000 requests, by training length",
                                "sub": ["Each dot is one round, vs. a cache trained on 5,000 requests (G1).", "Right of 0 is slower."]}},
        "warmup": warm_curve, "signal": signal_curve,
        "table": {"headers": ["Setup", "Startup, default CDS", "Startup, AppCDS", "Startup, AOT cache",
                              "AOT cache vs AppCDS: startup (95% CI)", "AOT cache vs AppCDS: 2,000 requests (95% CI)"],
                  "rows": table_rows},
        "warm_rows": warm_rows, "jdk26": jdk_rows,
        "sizes_mb": {"appcds_g1": mb("small", "appcds-g1.jsa"), "aot_g1": mb("small", "aot-g1.aot"),
                     "aot_g1_n0": mb("small", "aot-g1-n0.aot"), "aot_zgc_jdk25": mb("t25", "aot-zgc.aot"),
                     "aot_zgc_jdk26": mb("t26", "aot-zgc.aot"), "heavy_aot": mb("heavy", "aot-zgc.aot")},
        "train_s": {k: fmt(sizes[("small", a)]["train_seconds"], 1) for k, a in
                    (("appcds_g1", "appcds-g1.jsa"), ("aot_g1", "aot-g1.aot"), ("aot_g1_n0", "aot-g1-n0.aot"), ("aot_g1_n20000", "aot-g1-n20000.aot"))},
    }
    g1s, zs = start_rows[0], start_rows[3]
    out["aria"] = {
        "startup": f"With the AOT cache, startup was {g1s['shift']['label']} ms vs AppCDS on G1 ({g1s['faster']} of {g1s['n']} rounds faster), "
                   f"and {zs['shift']['label']} ms on ZGC with JDK 25.",
        "training": "Extra time for 2,000 requests vs a cache trained on 5,000 requests: " +
                    ", ".join(f"{r['setup']} training requests {r['shift']['label']} ms" for r in train_rows) + ".",
        "warmup": "On G1, requests got faster sooner with an AOT cache trained with requests; the cache trained only up to onRefresh "
                  "warmed up like AppCDS and default CDS.",
        "signal": "During training, new classes stopped appearing after about 500 requests; JIT compilations faded out by a few thousand.",
    }
    json.dump(out, open(OUT, "w"), indent=1, ensure_ascii=False)
    for r in start_rows + warm_rows + train_rows + jdk_rows:
        print(f"{r['setup']:22} {r['shift']['label']:>7} ms  CI {r['ci']['label']:>18}  {r['faster']}/{r['n']}  p={r['p']}")


if __name__ == "__main__":
    main()
