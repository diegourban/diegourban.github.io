#!/usr/bin/env python3
"""Single-run checks: how many pool connections each kind of method holds during the slow call, on an idle app.
Runs every check in Checks.java three times, for eager and lazy fetching, with open session in view off and on.
Writes checks.json."""
import json, os, sys

import common as c

CHECKS = ["none", "required", "readOnly", "notSupported", "never", "supports",
          "queryThenCall", "callThenQuery", "supportsQueryThenCall", "notSupportedQueryThenCall",
          "noneQueryThenCall", "suspended", "jdbcRequired", "jdbcReadOnly"]
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
    finally:
        c.stop_all()
    json.dump(out, open(os.path.join(c.HERE, "checks.json"), "w"), indent=1)


if __name__ == "__main__":
    sys.exit(main())
