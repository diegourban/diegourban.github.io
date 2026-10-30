#!/usr/bin/env python3
"""Load test: the mixed endpoint under eager and lazy connection fetching, and the narrowed reference, at stepped
arrival rates, for 10%, 50% and 90% of requests on the downstream path. Appends one line per step to results.jsonl.

  python3 bench.py ROUNDS          full run
  python3 bench.py --smoke         two short steps per combination, to check the harness (results_smoke.jsonl)
"""
import json, os, random, subprocess, sys, threading, time

import common as c

SETUPS = {  # name: (endpoint, connection fetching)
    "eager": ("mixed", "eager"),
    "lazy": ("mixed", "lazy"),
    "narrowed": ("narrowed", "eager"),
}
SHARES = [0.1, 0.5, 0.9]
RATES = [10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 125, 150, 200, 250, 300]
STEP = "30s"
FAIL = 0.01          # a step "fails" when more than 1% of its requests fail
STOP_AFTER = 3       # consecutive failing steps before moving on to the next combination


def k6(target, share, rate, duration):
    cmd = ["docker", "run", "--rm", "--network", c.NET, "--cpus=2", "-m", "1g",
           "-v", f"{os.path.join(c.HERE, 'load.js')}:/load.js:ro", c.K6_IMAGE, "run", "--quiet",
           "-e", f"RATE={rate}", "-e", f"SHARE={share}", "-e", f"TARGET={target}", "-e", f"DURATION={duration}", "/load.js"]
    out = subprocess.run(cmd, capture_output=True, text=True)
    try:
        return json.loads(out.stdout[out.stdout.index("{"):])
    except ValueError:
        raise RuntimeError(f"k6 gave no summary:\n{out.stderr[-2000:]}")


class PoolSampler(threading.Thread):
    """Samples the pool's active connections and waiting threads every half second during a step."""

    def __init__(self):
        super().__init__(daemon=True)
        self.stop, self.active, self.pending = threading.Event(), [], []

    def run(self):
        while not self.stop.is_set():
            try:
                self.active.append(c.metric("hikaricp.connections.active"))
                self.pending.append(c.metric("hikaricp.connections.pending"))
            except Exception:
                pass
            time.sleep(0.5)


def flatten(s):
    m = s["metrics"]
    reqs = m["http_reqs"]["values"]["count"]
    dropped = m.get("dropped_iterations", {}).get("values", {}).get("count", 0)
    failed = m["http_req_failed"]["values"]["rate"] * reqs
    # A request k6 never sent counts as failed: the app could not keep up with the arrival rate.
    row = {"reqs": reqs, "dropped": dropped, "failed_rate": (failed + dropped) / (reqs + dropped) if reqs + dropped else 0.0}
    for route in ["db", "downstream"]:
        d = m.get(f"http_req_duration{{route:{route}}}", {}).get("values", {})
        f = m.get(f"http_req_failed{{route:{route}}}", {}).get("values", {})
        row[route] = {"n": d.get("count", 0), "failed_rate": f.get("rate", 0.0),
                      **{k: d.get(k) for k in ["med", "p(90)", "p(95)", "p(99)", "max"]}}
    return row


def combination(rnd, setup, share, rates, duration, out):
    target, fetch = SETUPS[setup]
    c.start_app(fetch)
    k6(target, share, 20, "10s")  # warm-up, not recorded
    failing = 0
    for rate in rates:
        sampler = PoolSampler()
        sampler.start()
        t0 = time.time()
        summary = k6(target, share, rate, duration)
        sampler.stop.set()
        sampler.join()
        row = {"round": rnd, "setup": setup, "share": share, "rate": rate, "t": round(t0), **flatten(summary),
               "pool_active_max": max(sampler.active, default=None),
               "pool_pending_max": max(sampler.pending, default=None),
               "pool_pending_mean": sum(sampler.pending) / len(sampler.pending) if sampler.pending else None}
        out.write(json.dumps(row) + "\n")
        out.flush()
        print(f"r{rnd} {setup:8} share {share:.1f} {rate:3} req/s  failed {row['failed_rate']:6.1%}  "
              f"db p95 {row['db']['p(95)'] or 0:7.0f} ms  pending max {row['pool_pending_max']}", flush=True)
        failing = failing + 1 if row["failed_rate"] > FAIL else 0
        if failing >= STOP_AFTER:
            break
        time.sleep(3)  # let queues drain between steps


def main():
    smoke = "--smoke" in sys.argv
    rounds = 1 if smoke else int(sys.argv[1])
    rates, duration = ([20, 60], "10s") if smoke else (RATES, STEP)
    name = "results_smoke.jsonl" if smoke else "results.jsonl"
    c.build_image()
    c.start_infra()
    try:
        with open(os.path.join(c.HERE, name), "a") as out:
            for rnd in range(1, rounds + 1):
                combos = [(s, sh) for s in SETUPS for sh in SHARES]
                random.shuffle(combos)
                for setup, share in combos:
                    combination(rnd, setup, share, rates, duration, out)
    finally:
        c.stop_all()


if __name__ == "__main__":
    main()
