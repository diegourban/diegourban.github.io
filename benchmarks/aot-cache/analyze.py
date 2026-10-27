#!/usr/bin/env python3
"""Paired analysis of results.jsonl, the warm-up curves, and the training signal. Writes analysis.json.

  python3 analyze.py [results.jsonl [results_jdk.jsonl]]

Same method as the earlier posts: every pair of variants is compared within a round; the shift is the
Hodges-Lehmann estimate of the paired differences, its 95% CI a bootstrap of that estimate (fixed seed), and p an
exact Wilcoxon signed-rank test. Positive shift = the first variant of the pair is slower / larger.
Warm-up metrics per run: the first request, the sum of all 2,000 latencies, and the median latency of requests
201-500 and 1,501-2,000 per request type (the workload alternates a GET and a POST that fails validation, about twice
as fast, so a median over both lands between them). MDE: 2.8 * SD of the paired differences / sqrt(n), the smallest
shift ten pairs detect with 80% power at alpha 0.05.
The JDK 25 vs 26 comparison comes from results_jdk.jsonl, a second session that ran all six Temurin ZGC variants
(default CDS, AppCDS, AOT cache on each JDK) in the same rounds; the Temurin runs in results.jsonl aren't used.
"""
import json, os, random, statistics as st, sys
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "default-cds"))
from paired_stats import hodges_lehmann, wilcoxon_p  # noqa: E402

SEED, BOOT = 20261027, 10000
GCS = ("zgc", "g1", "parallel", "serial")
PAIRS = [(f"aot_{g}", f"appcds_{g}") for g in GCS] + [(f"appcds_{g}", f"cds_{g}") for g in GCS] + \
        [(f"aot_{g}", f"cds_{g}") for g in GCS] + \
        [("heavy_aot_zgc", "heavy_appcds_zgc"), ("heavy_appcds_zgc", "heavy_cds_zgc"), ("heavy_aot_zgc", "heavy_cds_zgc")] + \
        [(f"aot_g1_n{n}", "aot_g1") for n in (0, 200, 1000, 20000)] + [("aot_g1_n0", "appcds_g1")]
JDK_PAIRS = [(f"t{j}_aot_zgc", f"t{j}_appcds_zgc") for j in (25, 26)] + [(f"t{j}_appcds_zgc", f"t{j}_cds_zgc") for j in (25, 26)] + \
            [(f"t{j}_aot_zgc", f"t{j}_cds_zgc") for j in (25, 26)] + \
            [("t26_aot_zgc", "t25_aot_zgc"), ("t26_appcds_zgc", "t25_appcds_zgc"), ("t26_cds_zgc", "t25_cds_zgc")]
METRICS = ("process_s", "ready_s", "first_ms", "get_201_500_ms", "post_201_500_ms", "get_1501_2000_ms", "post_1501_2000_ms",
           "warmup_total_s", "rss_ready_mb", "rss_warm_mb", "anon_warm_mb", "file_warm_mb", "cpu_warm_s")



def metrics(r):
    lat = r["latency_us"]
    a, w = r["after_ready"], r["after_warmup"]
    return {"process_s": r["process"], "ready_s": r["ready_wall"], "first_ms": lat[0] / 1000,
            # After the seed requests, even indexes are GETs and odd ones the failing POSTs (common.requests).
            "get_201_500_ms": st.median(lat[200:500:2]) / 1000, "post_201_500_ms": st.median(lat[201:500:2]) / 1000,
            "get_1501_2000_ms": st.median(lat[1500:2000:2]) / 1000, "post_1501_2000_ms": st.median(lat[1501:2000:2]) / 1000,
            "warmup_total_s": sum(lat) / 1e6, "rss_ready_mb": a["rss"], "rss_warm_mb": w["rss"],
            "anon_warm_mb": w["rss_anon"], "file_warm_mb": w["rss_file"], "cpu_warm_s": w["cpu_s"]}


def boot_hl(d, rng):
    m = sorted(hodges_lehmann(rng.choices(d, k=len(d))) for _ in range(BOOT))
    return m[int(.025 * BOOT)], m[int(.975 * BOOT)]


def load(path):
    recs = [json.loads(l) for l in open(path)]
    by = defaultdict(dict)  # variant -> round -> metrics
    for r in recs:
        if not r.get("failed"):
            by[r["variant"]][r["round"]] = metrics(r)
    return recs, by


def pairs(by, plist, rng):
    out = []
    for a, b in plist:
        common = sorted(set(by[a]) & set(by[b]))
        for m in METRICS:
            d = [by[a][k][m] - by[b][k][m] for k in common]
            if len(d) < 2:
                continue
            lo, hi = boot_hl(d, rng)
            out.append({"a": a, "b": b, "metric": m, "n": len(d), "shift": hodges_lehmann(d), "ci": [lo, hi],
                        "a_smaller": sum(x < 0 for x in d), "p": wilcoxon_p(d, rng),
                        "mde": 2.8 * st.stdev(d) / len(d) ** 0.5})
    return out


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "results.jsonl")
    jdk_path = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, "results_jdk.jsonl")
    recs = [json.loads(l) for l in open(path)]
    failed = [r for r in recs if r.get("failed")]
    by = defaultdict(dict)  # variant -> round -> metrics
    curves = defaultdict(list)
    for r in recs:
        if r.get("failed"):
            continue
        by[r["variant"]][r["round"]] = metrics(r)
        curves[r["variant"]].append(r["latency_us"])
    rng = random.Random(SEED)
    out = {"seed": SEED, "boot": BOOT, "rounds": len({r["round"] for r in recs}), "failed_runs": len(failed),
           "medians": {v: {m: st.median(x[m] for x in rounds.values()) for m in next(iter(rounds.values()))}
                       for v, rounds in sorted(by.items())},
           "pairs": []}
    out["pairs"] = pairs(by, PAIRS, rng)
    if os.path.exists(jdk_path):
        jrecs, jby = load(jdk_path)
        out["jdk_rounds"] = len({r["round"] for r in jrecs})
        out["jdk_failed_runs"] = sum(1 for r in jrecs if r.get("failed"))
        out["jdk_medians"] = {v: {m: st.median(x[m] for x in rounds.values()) for m in METRICS} for v, rounds in sorted(jby.items())}
        out["jdk_pairs"] = pairs(jby, JDK_PAIRS, rng)
    # Warm-up curves: per variant, the median over rounds of each request's latency, in buckets of 20 requests.
    out["curves_ms"] = {v: [round(st.median(st.median(run[i:i + 20]) for run in runs) / 1000, 3)
                            for i in range(0, len(runs[0]), 20)] for v, runs in curves.items()}
    # Training signal: class loads and JIT compilations per 100-request window of the logged training run.
    sig = json.load(open(os.path.join(HERE, "signal.json")))
    edges = [sig["workload_start_uptime"]] + [t for t, _ in sig["marks"]]
    count = lambda ts, lo, hi: sum(lo <= t < hi for t in ts)
    out["signal"] = [{"requests": n, "classes": count(sig["classes"], lo, hi), "compilations": count(sig["compilations"], lo, hi)}
                     for (lo, hi, (_, n)) in zip(edges, edges[1:], sig["marks"])]
    total = sum(x["compilations"] for x in out["signal"])
    out["signal_share"] = {f"compilations_by_{n}": round(sum(x["compilations"] for x in out["signal"] if x["requests"] <= n) / total, 3)
                           for n in (200, 1000, 2000, 5000)}
    out["signal_share"]["classes_after_500"] = sum(x["classes"] for x in out["signal"] if x["requests"] > 500)
    out["signal_share"]["compilations_after_5000"] = sum(x["compilations"] for x in out["signal"] if x["requests"] > 5000)
    json.dump(out, open(os.path.join(HERE, "analysis.json"), "w"), indent=1)
    print(f"{out['rounds']} rounds, {len(recs)} runs, {len(failed)} failed")
    for p in out["pairs"] + out.get("jdk_pairs", []):
        if p["metric"] in ("process_s", "first_ms", "get_201_500_ms", "post_201_500_ms", "warmup_total_s", "rss_warm_mb"):
            print(f"{p['a']:16} vs {p['b']:16} {p['metric']:16} {p['shift']:+9.3f} [{p['ci'][0]:+.3f}, {p['ci'][1]:+.3f}] "
                  f"{p['a_smaller']}/{p['n']} p={p['p']:.3f} mde={p['mde']:.3f}")


if __name__ == "__main__":
    main()
