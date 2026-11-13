#!/usr/bin/env python3
"""Deterministic two-transaction checks on a 1-seat event: T1 books the seat and pauses before its commit, T2 runs
up to its lock while T1 is open, then T1 commits. A writer is safe when T2 finds the event full.

  checks.py              all variants on MySQL (REPEATABLE READ), the main ones on PostgreSQL (READ COMMITTED and
                         REPEATABLE READ); writes checks.json
"""
import json, os, sys

from common import HERE, build_jar, run_app, start_dbs, stop_dbs

MYSQL_ALL = ["noLock", "plainReadFirst", "lockFirst", "readCommitted", "counter", "subquery", "subqueryFirst",
             "existsFirst", "referenceFirst", "counterEventLoadedFirst", "outerRead", "outerReadNewTransaction"]
PG = ["plainReadFirst", "lockFirst", "counter", "subquery", "counterEventLoadedFirst"]


def main():
    build_jar()
    start_dbs()
    results = []
    try:
        runs = [("mysql", MYSQL_ALL, None), ("postgresql", PG, None), ("postgresql", PG, "TRANSACTION_REPEATABLE_READ")]
        for db, variants, isolation in runs:
            for r in run_app(db, "--mode=check", "--variants=" + ",".join(variants), isolation=isolation):
                results.append(r)
                print(f"{r['db']:<10} {r['isolation']:<16} {r['variant']:<24} bookings={r['bookings']} "
                      f"waits on [{r['t2WaitingFor'][:60]}] t2={r['t2'][:80]}", flush=True)
    finally:
        stop_dbs()
    with open(os.path.join(HERE, "checks.json"), "w") as f:
        json.dump(results, f, indent=1)


if __name__ == "__main__":
    sys.exit(main())
