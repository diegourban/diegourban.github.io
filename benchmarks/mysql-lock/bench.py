#!/usr/bin/env python3
"""The hammer: every setup under two kinds of load, in random order, appending one JSON line per step to results.jsonl.

  arrivals  100 events of 10 seats, 15 requests per event arriving at random (Poisson) at 10..1000 requests/s per
            event, one attempt each. The arrival pattern is seeded by the round, so every setup sees the same one.
  flash     200 events of 10 seats, c bookers (2..50) retrying until the event says it is full.

  bench.py [rounds]      default 3
"""
import json, os, random, sys, time

from common import HERE, build_jar, run_app, start_dbs, stop_dbs

SETUPS = [("mysql", "plainReadFirst"), ("mysql", "lockFirst"), ("mysql", "readCommitted"), ("mysql", "counter"),
          ("mysql", "subquery"), ("postgresql", "plainReadFirst")]
RATES = [10, 20, 50, 100, 200, 500, 1000]
CONCURRENCY = [2, 5, 10, 20, 50]


def main():
    rounds = int(sys.argv[1]) if len(sys.argv) > 1 else 3
    build_jar()
    start_dbs()
    out = os.path.join(HERE, "results.jsonl")
    try:
        for rnd in range(1, rounds + 1):
            order = SETUPS[:]
            random.shuffle(order)
            for db, variant in order:
                rates = RATES[:]
                random.shuffle(rates)
                conc = CONCURRENCY[:]
                random.shuffle(conc)
                lines = run_app(db, "--mode=arrivals", f"--variant={variant}", "--events=100", f"--seed={rnd}",
                                "--rates=" + ",".join(map(str, rates)))
                lines += run_app(db, "--mode=hammer", f"--variant={variant}", "--events=200",
                                 "--concurrency=" + ",".join(map(str, conc)))
                with open(out, "a") as f:
                    for r in lines:
                        r["round"] = rnd
                        r["mode"] = "flash" if r["mode"] == "hammer" else r["mode"]
                        f.write(json.dumps(r) + "\n")
                print(time.strftime("%H:%M:%S"), f"round {rnd} {db} {variant}: " + ", ".join(
                    f"{r.get('rate', r.get('concurrency'))}:{r['overbooked']}" for r in lines), flush=True)
    finally:
        stop_dbs()


if __name__ == "__main__":
    sys.exit(main())
