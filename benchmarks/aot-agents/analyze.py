#!/usr/bin/env python3
"""Paired analysis of results.jsonl with the aot-cache method (Hodges-Lehmann shift, bootstrap CI, exact Wilcoxon, MDE).
Writes analysis.json. Positive shift = the first variant is slower."""
import json, os, random, statistics as st, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "aot-cache"))
import analyze as base  # noqa: E402

PAIRS = [
    ("otel_cds_g1", "cds_g1"),                      # what the agent costs
    ("appcds_g1", "cds_g1"),                        # AppCDS without the agent
    ("otel_appcds_g1", "otel_cds_g1"),              # AppCDS with the agent, archive trained with it
    ("otel_appcds_g1", "appcds_g1"),
    ("otel_appcds_noflag_g1", "otel_cds_g1"),       # same archive, allow flag missing at runtime
    ("otel_appcds_untrained_g1", "otel_cds_g1"),    # archive trained without the agent
    ("otel_aot_untrained_g1", "otel_cds_g1"),       # post 3's trap
    ("aot_g1", "cds_g1"),
]


def main():
    recs, by = base.load(os.path.join(HERE, "results.jsonl"))
    rng = random.Random(base.SEED)
    out = {"seed": base.SEED, "boot": base.BOOT, "rounds": len({r["round"] for r in recs}),
           "failed_runs": sum(1 for r in recs if r.get("failed")),
           "medians": {v: {m: st.median(x[m] for x in rounds.values()) for m in base.METRICS} for v, rounds in sorted(by.items())},
           "pairs": base.pairs(by, PAIRS, rng)}
    json.dump(out, open(os.path.join(HERE, "analysis.json"), "w"), indent=1)
    print(f"{out['rounds']} rounds, {len(recs)} runs, {out['failed_runs']} failed")
    for v, m in out["medians"].items():
        print(f"  {v:26} startup {m['process_s']:.2f} s, 2,000 requests {m['warmup_total_s']:.2f} s")
    for p in out["pairs"]:
        if p["metric"] in ("process_s", "warmup_total_s"):
            k = 1000
            print(f"{p['a']:26} vs {p['b']:14} {p['metric']:15} {p['shift']*k:+7.0f} ms [{p['ci'][0]*k:+.0f}, {p['ci'][1]*k:+.0f}] "
                  f"{p['a_smaller']}/{p['n']} p={p['p']:.3f} mde={p['mde']*k:.0f}")


if __name__ == "__main__":
    main()
