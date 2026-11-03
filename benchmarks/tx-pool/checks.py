#!/usr/bin/env python3
"""Single-run checks: how many pool connections each kind of method holds during the slow call, on an idle app.
Runs every check in Checks.java three times, for eager and lazy fetching, with open session in view off and on.
Then a contention check with a pool of 1. Writes checks.json."""
import json, os, sys, threading, time

import common as c

CHECKS = ["none", "required", "readOnly", "notSupported", "never", "supports",
          "queryThenCall", "callThenQuery", "supportsQueryThenCall", "notSupportedQueryThenCall",
          "noneQueryThenCall", "suspended", "jdbcRequired", "jdbcReadOnly",
          "emSupportsQueryThenCall", "emNotSupportedQueryThenCall", "emNoneQueryThenCall", "requiresNewInsideQueried"]
REPEATS = 3


def main():
    c.build_image()
    c.start_infra()
    out = {}
    try:
        for fetch in ["eager", "lazy"]:
            for osiv in [False, True]:
                c.start_app(fetch, osiv)
                key = f"{fetch}{'_osiv' if osiv else ''}"
                out[key] = {}
                for name in CHECKS:
                    seen = [c.get(f"/check/{name}")["activeDuringCall"] for _ in range(REPEATS)]
                    out[key][name] = seen
                    print(f"{key:12} {name:28} {seen}", flush=True)
        # Contention, pool of 1: one request holds the only connection for 4 s, a second runs callThenSave
        # (the slow call, then a write). Where does the second one fail: before or after the downstream call?
        for fetch in ["eager", "lazy"]:
            c.start_app(fetch, env={"SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE": 1, "SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE": 1})
            holder = threading.Thread(target=lambda: c.get("/hold?ms=4000", timeout=10))
            holder.start()
            time.sleep(0.5)
            out[f"contention_{fetch}"] = c.get("/callThenSave", timeout=10)
            holder.join()
            print(f"contention {fetch:6} {out[f'contention_{fetch}']}", flush=True)
    finally:
        c.stop_all()
    json.dump(out, open(os.path.join(c.HERE, "checks.json"), "w"), indent=1)


if __name__ == "__main__":
    sys.exit(main())
