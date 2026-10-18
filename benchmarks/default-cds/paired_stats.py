#!/usr/bin/env python3
"""Paired A/B statistics for benchmark results, stdlib only (no scipy).

Input: a JSON array of run records, in execution order. Each record has a
variant field and numeric metric fields. Runs are grouped into blocks
(rounds): either by an explicit --block-key field, or by position, where
every --block-size consecutive records form one round (the shape a
"shuffle the variants, run each once, repeat N times" benchmark produces).

For each --pair A:B and each metric it prints, as a markdown table:
  median A, median B, Hodges-Lehmann paired shift (B minus A), bootstrap 95%
  CI of the median paired difference, count of pairs where B < A, exact
  Wilcoxon signed-rank p, exact sign-flip p, and the minimum detectable
  effect (80% power, alpha 0.05).
Then a pooled relative shift across all pairs (metric relative to the A
median of its pair), and drift / position checks on residuals.

Usage:
  paired_stats.py --file results.json --pair xshare_off:cds_default \
      --metrics process,rss_anon [--block-size 3 | --block-key round] \
      [--variant-key variant] [--seed 1]
  paired_stats.py --file results.json --list      # show variants + metrics
  paired_stats.py --self-test
"""
import argparse, itertools, json, random, statistics as st, sys

EXACT_MAX_N = 20


def ranks_abs(d):
    a = sorted(abs(x) for x in d)
    return [(sum(v < abs(x) for v in a) + sum(v <= abs(x) for v in a) + 1) / 2 for x in d]


def wilcoxon_p(diffs, rng):
    d = [x for x in diffs if x != 0]
    n = len(d)
    if n == 0:
        return 1.0
    rk = ranks_abs(d)
    mean = n * (n + 1) / 4
    obs = abs(sum(r for r, x in zip(rk, d) if x > 0) - mean)
    if n <= EXACT_MAX_N:
        hits = sum(abs(sum(r for r, s in zip(rk, signs) if s) - mean) >= obs - 1e-9
                   for signs in itertools.product((0, 1), repeat=n))
        return hits / 2 ** n
    B = 20000
    hits = sum(abs(sum(r for r in rk if rng.random() < .5) - mean) >= obs - 1e-9 for _ in range(B))
    return (hits + 1) / (B + 1)


def signflip_p(diffs, rng):
    n = len(diffs)
    obs = abs(sum(diffs))
    if n <= EXACT_MAX_N:
        hits = sum(abs(sum(s * x for s, x in zip(signs, diffs))) >= obs - 1e-12
                   for signs in itertools.product((1, -1), repeat=n))
        return hits / 2 ** n
    B = 20000
    hits = sum(abs(sum(x if rng.random() < .5 else -x for x in diffs)) >= obs - 1e-12 for _ in range(B))
    return (hits + 1) / (B + 1)


def hodges_lehmann(d):
    return st.median([(d[i] + d[j]) / 2 for i in range(len(d)) for j in range(i, len(d))])


def boot_ci(d, rng, B=20000):
    m = sorted(st.median(rng.choices(d, k=len(d))) for _ in range(B))
    return m[int(.025 * B)], m[int(.975 * B)]


def corr(x, y):
    mx, my = st.mean(x), st.mean(y)
    den = (sum((a - mx) ** 2 for a in x) * sum((b - my) ** 2 for b in y)) ** .5
    return 0.0 if den == 0 else sum((a - mx) * (b - my) for a, b in zip(x, y)) / den


def assign_blocks(recs, vkey, block_size, block_key):
    nvar = len({r[vkey] for r in recs})
    size = block_size or nvar
    for i, r in enumerate(recs):
        r["_block"] = r[block_key] if block_key else i // size
        r["_pos"] = i % size
        r["_idx"] = i
    if not block_key and len(recs) % size:
        print(f"WARNING: {len(recs)} records is not a multiple of block size {size}; "
              "positional blocking may be wrong, pass --block-size or --block-key", file=sys.stderr)
    return size


def pair_diffs(recs, vkey, a, b, metric):
    A = {r["_block"]: r[metric] for r in recs if r[vkey] == a and r.get(metric) is not None}
    B = {r["_block"]: r[metric] for r in recs if r[vkey] == b and r.get(metric) is not None}
    keys = sorted(set(A) & set(B))
    return [A[k] for k in keys], [B[k] for k in keys]


def fmt(x):
    return f"{x:.4g}"


def analyse(recs, vkey, pairs, metrics, rng):
    print("| pair | metric | n | median A | median B | HL shift B-A | boot 95% CI | B<A | Wilcoxon p | sign-flip p | MDE (80%) | MDE % of A |")
    print("|---|---|---|---|---|---|---|---|---|---|---|---|")
    pooled = {}
    for m in metrics:
        for a, b in pairs:
            av, bv = pair_diffs(recs, vkey, a, b, m)
            if len(av) < 3:
                print(f"| {a}→{b} | {m} | {len(av)} | too few paired blocks | | | | | | | | |")
                continue
            d = [y - x for x, y in zip(av, bv)]
            lo, hi = boot_ci(d, rng)
            sd = st.stdev(d) if len(d) > 1 else 0.0
            mde = 2.8 * sd / len(d) ** .5
            ma = st.median(av)
            print(f"| {a}→{b} | {m} | {len(d)} | {fmt(ma)} | {fmt(st.median(bv))} | {hodges_lehmann(d):+.4g} | "
                  f"[{lo:+.4g}, {hi:+.4g}] | {sum(x < 0 for x in d)}/{len(d)} | {wilcoxon_p(d, rng):.4f} | "
                  f"{signflip_p(d, rng):.4f} | {mde:.4g} | {100 * mde / ma if ma else float('nan'):.1f}% |")
            if ma:
                pooled.setdefault(m, []).extend(x / ma for x in d)
    if len(pairs) > 1:
        print("\nPooled relative shift (naive: ignores clustering by pair, widen mentally or fit a mixed model):\n")
        print("| metric | pairs | median rel. shift | boot 95% CI | B<A |")
        print("|---|---|---|---|---|")
        for m, d in pooled.items():
            lo, hi = boot_ci(d, rng)
            print(f"| {m} | {len(d)} | {100 * st.median(d):+.2f}% | [{100 * lo:+.2f}%, {100 * hi:+.2f}%] | {sum(x < 0 for x in d)}/{len(d)} |")


def drift(recs, vkey, metric):
    by = {}
    for r in recs:
        if r.get(metric) is not None:
            by.setdefault(r[vkey], []).append(r[metric])
    rs = [r for r in recs if r.get(metric) is not None]
    res = [r[metric] - st.median(by[r[vkey]]) for r in rs]
    if len(rs) < 4:
        return
    print(f"\nDrift / order checks on `{metric}` residuals (value minus its variant median):")
    print(f"- corr(execution index, residual) = {corr([r['_idx'] for r in rs], res):+.2f}  (|r| > 0.3: time drift, report it)")
    print(f"- corr(position in block, residual) = {corr([r['_pos'] for r in rs], res):+.2f}  (|r| > 0.3: order effect)")
    first = [x for r, x in zip(rs, res) if r['_block'] == rs[0]['_block']]
    print(f"- first-block median residual = {st.median(first):+.4g} vs overall 0  (large positive: warm-up effect)")
    top = sorted(((x, r[vkey], r['_block']) for r, x in zip(rs, res)), reverse=True)[:3]
    print("- largest residuals: " + ", ".join(f"{x:+.3g} ({v}, block {b})" for x, v, b in top))


def self_test():
    rng = random.Random(7)
    fails = 0

    def check(name, cond):
        nonlocal fails
        print(("PASS " if cond else "FAIL ") + name)
        fails += not cond

    check("wilcoxon all-negative n=10 gives p=2/1024", abs(wilcoxon_p([-i for i in range(1, 11)], rng) - 2 / 1024) < 1e-12)
    check("signflip all-negative n=10 gives p=2/1024", abs(signflip_p([-1.0] * 10, rng) - 2 / 1024) < 1e-12)
    check("wilcoxon symmetric diffs gives p=1", wilcoxon_p([-1, 1, -2, 2], rng) == 1.0)
    check("hodges-lehmann of constant shift", hodges_lehmann([-3.0] * 6) == -3.0)
    lo, hi = boot_ci([-5.0, -4.0, -6.0, -5.0, -5.5, -4.5], rng)
    check("bootstrap CI brackets the median and excludes 0", lo <= -5.0 <= hi and hi < 0)
    # synthetic blocked benchmark: B is 50 ms faster, 10 rounds of 2 variants, shuffled
    recs = []
    for i in range(10):
        order = ["a", "b"]
        rng.shuffle(order)
        for v in order:
            recs.append({"variant": v, "t": 2.0 + rng.gauss(0, .02) - (.05 if v == "b" else 0)})
    assign_blocks(recs, "variant", None, None)
    av, bv = pair_diffs(recs, "variant", "a", "b", "t")
    d = [y - x for x, y in zip(av, bv)]
    check("synthetic -50ms recovered by HL within 30ms", abs(hodges_lehmann(d) + .05) < .03)
    check("synthetic -50ms is significant", wilcoxon_p(d, rng) < .01)
    check("blocks pair one a with one b", len(d) == 10)
    print(f"{'ALL PASS' if not fails else f'{fails} FAILED'}")
    return 1 if fails else 0


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--file")
    p.add_argument("--pair", action="append", default=[], help="A:B, repeatable; shift is B minus A")
    p.add_argument("--metrics", default="")
    p.add_argument("--variant-key", default="variant")
    p.add_argument("--block-size", type=int)
    p.add_argument("--block-key")
    p.add_argument("--seed", type=int, default=1)
    p.add_argument("--list", action="store_true")
    p.add_argument("--self-test", action="store_true")
    o = p.parse_args()
    if o.self_test:
        sys.exit(self_test())
    if not o.file:
        p.error("--file is required")
    recs = json.load(open(o.file))
    if o.list:
        print("variants:", sorted({r[o.variant_key] for r in recs}))
        print("numeric fields:", sorted({k for r in recs for k, v in r.items() if isinstance(v, (int, float)) and not isinstance(v, bool)}))
        print("records:", len(recs))
        return
    if not o.pair or not o.metrics:
        p.error("--pair and --metrics are required (use --list to see options)")
    size = assign_blocks(recs, o.variant_key, o.block_size, o.block_key)
    pairs = [tuple(x.split(":", 1)) for x in o.pair]
    metrics = [m for m in o.metrics.split(",") if m]
    print(f"Blocking: {'key ' + o.block_key if o.block_key else f'positional, {size} records per block'}; seed {o.seed}\n")
    analyse(recs, o.variant_key, pairs, metrics, random.Random(o.seed))
    drift(recs, o.variant_key, metrics[0])


if __name__ == "__main__":
    main()
