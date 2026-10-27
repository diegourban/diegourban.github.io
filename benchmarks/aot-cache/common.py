"""Shared pieces of the AOT cache benchmark: the request workload, Docker and Postgres helpers.

The workload keeps the cost of each request constant, so latency over time reflects the JVM warming up and not
the data growing: a few valid POSTs seed a fixed data set, then GETs alternate with POSTs that fail validation.
Training runs and measured runs use the same generator; training runs are just longer.
"""
import http.client, json, subprocess, time

SMALL_JAR = "/app/cdsdemo-0.0.1-SNAPSHOT.jar"
HEAVY_JAR = "/app/heavydemo-0.0.1-SNAPSHOT.jar"
NETWORK = "aotbench"
PG = "aotbench-pg"
PG_IMAGE = "postgres:17.6"  # the version the post-2 heavy-app runs used
HEAVY_ENTITIES = [f"entity{i:02d}" for i in range(1, 11)]
LIMITS = ["--cpus=2", "-m", "1g"]

# Lets a training run end itself cleanly over HTTP (the cache is written when the JVM exits).
SHUTDOWN_PROPS = ["-Dmanagement.endpoints.web.exposure.include=health,metrics,shutdown",
                  "-Dmanagement.endpoint.shutdown.access=unrestricted"]


def requests(app, n):
    """The first n requests of the workload, as (method, path, body) tuples."""
    out = []
    if app == "small":
        seed = [("POST", "/items", {"name": f"item {i}"}) for i in range(50)]
        cycle = [("GET", "/items", None), ("POST", "/items", {"name": ""})]
    else:
        seed = [("POST", f"/api/{e}", {"name": f"{e} item {i}", "status": "NEW"}) for i in range(5) for e in HEAVY_ENTITIES]
        cycle = [r for e in HEAVY_ENTITIES for r in (
            ("GET", f"/api/{e}", None), ("GET", f"/api/{e}/search?q=item", None), ("POST", f"/api/{e}", {"name": ""}))]
    out = seed[:n]
    while len(out) < n:
        out += cycle[: n - len(out)]
    return out


def drive(port, reqs):
    """Sends the requests over one keep-alive connection; returns per-request latency in microseconds."""
    conn = http.client.HTTPConnection("localhost", port, timeout=30)
    lat = []
    for method, path, body in reqs:
        data = json.dumps(body) if body is not None else None
        headers = {"Content-Type": "application/json"} if data else {}
        t = time.perf_counter_ns()
        conn.request(method, path, body=data, headers=headers)
        resp = conn.getresponse()
        resp.read()
        lat.append((time.perf_counter_ns() - t) // 1000)
        ok = resp.status == 400 if body == {"name": ""} else 200 <= resp.status < 300
        if not ok:
            raise RuntimeError(f"{method} {path} -> {resp.status}")
    conn.close()
    return lat


def sh(*args, check=True):
    return subprocess.run(args, capture_output=True, text=True, check=check).stdout


def get_json(port, path, timeout=2):
    conn = http.client.HTTPConnection("localhost", port, timeout=timeout)
    conn.request("GET", path)
    r = conn.getresponse()
    body = r.read()
    conn.close()
    return json.loads(body) if r.status == 200 else None


def wait_ready(port, deadline_s):
    """Seconds until /actuator/health is UP, measured from now; None on timeout."""
    t0 = time.monotonic()
    while time.monotonic() - t0 < deadline_s:
        try:
            if (get_json(port, "/actuator/health") or {}).get("status") == "UP":
                return time.monotonic() - t0
        except Exception:
            pass
        time.sleep(0.05)
    return None


def postgres_up():
    """Starts the shared Postgres on its own network (idempotent)."""
    if sh("docker", "network", "ls", "-q", "-f", f"name=^{NETWORK}$").strip() == "":
        sh("docker", "network", "create", NETWORK)
    if sh("docker", "ps", "-q", "-f", f"name=^{PG}$").strip() == "":
        sh("docker", "rm", "-f", PG, check=False)
        sh("docker", "run", "-d", "--name", PG, "--network", NETWORK, "--network-alias", "pg",
           "-e", "POSTGRES_DB=heavy", "-e", "POSTGRES_USER=heavy", "-e", "POSTGRES_PASSWORD=benchonly", PG_IMAGE)
        for _ in range(60):
            if subprocess.run(["docker", "exec", PG, "pg_isready", "-U", "heavy", "-d", "heavy"], capture_output=True).returncode == 0:
                return
            time.sleep(0.5)
        raise RuntimeError("postgres did not become ready")


def postgres_reset():
    """Empties the workload's tables (CASCADE also empties the tables that reference them), if they exist yet."""
    tables = ", ".join(HEAVY_ENTITIES)
    subprocess.run(["docker", "exec", PG, "psql", "-q", "-U", "heavy", "-d", "heavy", "-c",
                    f"DO $$ BEGIN IF to_regclass('entity01') IS NOT NULL THEN "
                    f"TRUNCATE {tables} RESTART IDENTITY CASCADE; END IF; END $$;"], capture_output=True, check=True)


def postgres_down():
    sh("docker", "rm", "-f", PG, check=False)
    sh("docker", "network", "rm", NETWORK, check=False)
