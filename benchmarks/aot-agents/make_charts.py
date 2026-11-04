#!/usr/bin/env python3
"""Chart and table data for the OpenTelemetry agent post: content/posts/aot-agents/charts.json.

One paired-shift chart (startup of every variant against no agent and default CDS, paired by round) and one table
(median startup, classes loaded from the archive in checks.json, shift against the agent with default CDS).
Reuses the aot-cache formatting and pairing so the numbers are computed the same way as post 3's.
"""
import json, os, random, statistics as st, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "aot-cache"))
import make_charts as base  # noqa: E402

OUT = os.path.join(HERE, "..", "..", "content", "posts", "aot-agents", "charts.json")
LABELS = {
    "aot_g1": "AOT cache, no agent",
    "appcds_g1": "AppCDS, no agent",
    "otel_cds_g1": "Agent, default CDS",
    "otel_appcds_g1": "Agent, AppCDS trained with it",
    "otel_appcds_untrained_g1": "Agent, AppCDS trained without it",
    "otel_appcds_noflag_g1": "Agent, AppCDS, flag missing at runtime",
    "otel_aot_untrained_g1": "Agent, AOT cache trained without it",
}


def main():
    by = base.load(os.path.join("..", "aot-agents", "results.jsonl"))
    checks = json.load(open(os.path.join(HERE, "checks.json")))
    rng = random.Random(base.SEED)
    startup = lambda r: r["process"] * 1000
    rows = [base.paired_row(by, v, "cds_g1", startup, label, rng) for v, label in LABELS.items()]
    vs_agent = {v: base.paired_row(by, v, "otel_cds_g1", startup, LABELS[v], rng) for v in LABELS if v.startswith("otel_") and v != "otel_cds_g1"}
    # The bootstrap draws above run in a different order than analyze.py's, so the CI edges can differ by a few ms.
    # Where analysis.json has the same pair, take its CI so the post, the table and the chart quote one number.
    ci = {(p["a"], p["b"]): p["ci"] for p in json.load(open(os.path.join(HERE, "analysis.json")))["pairs"] if p["metric"] == "process_s"}
    for b, row in [("cds_g1", r) for r in rows] + [("otel_cds_g1", r) for r in vs_agent.values()]:
        v = next(k for k, label in LABELS.items() if label == row["setup"])
        if (v, b) in ci:
            lo, hi = (x * 1000 for x in ci[(v, b)])
            row["ci"] = {"lo": float(base.rnd(lo)), "hi": float(base.rnd(hi)),
                         "label": f"{base.fmt(lo, sign=True)} to {base.fmt(hi, sign=True)}"}
    med = lambda v: st.median(r["process"] for r in by[v].values())
    table = []
    for v in ["cds_g1", *LABELS]:
        c = checks[v]
        shift = vs_agent.get(v)
        table.append([LABELS.get(v, "No agent, default CDS"), f"{base.fmt(med(v), 2)} s",
                      f"{c['classes_from_cache']:,} of {c['classes_loaded']:,}",
                      f"{shift['shift']['label']} ms ({shift['ci']['label']})" if shift else ""])
    table.append(["Agent, AOT cache trained with it", "no cache", "0 of 15 trainings wrote one", ""])
    out = {
        "generated_by": "benchmarks/aot-agents/make_charts.py", "seed": base.SEED,
        "startup": {"rows": rows, "axis": {"min": -1200, "max": 1800, "step": 600},
                    "titles": {"title": "Startup against no agent and default CDS",
                               "sub": ["Small app, G1, JDK 25. Each dot is one round. Left of 0 is faster.",
                                       "Bar: 95% CI · tick: Hodges-Lehmann estimate"]}},
        "table": {"headers": ["Setup", "Startup (median)", "Classes from the archive", "vs agent with default CDS (95% CI)"],
                  "rows": table},
        "vs_agent": vs_agent,
        "aria": {"startup": "The OpenTelemetry agent added about 1.6 s of startup. Without the agent the AOT cache saved about "
                            "1 s and AppCDS 0.8 s; with it, the best archive saved about 0.2 s."},
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    json.dump(out, open(OUT, "w"), indent=1, ensure_ascii=False)
    for r in rows:
        print(f"{r['setup']:40} {r['shift']['label']:>7} ms  CI {r['ci']['label']:>18}  {r['faster']}/{r['n']}")
    for r in table:
        print(" | ".join(r))


if __name__ == "__main__":
    main()
