"""Shared setup for the transaction pool benchmark: one Docker network with PostgreSQL, WireMock (300 ms delay),
the app, and k6 as the load generator. Each gets its own CPUs so the sender never competes with the app."""
import json, os, subprocess, time, urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
NET = "txbench"
APP_IMAGE = "txdemo:bench"
PG_IMAGE = "postgres:17.6"
WM_IMAGE = "wiremock/wiremock:3.13.2"
K6_IMAGE = "grafana/k6:1.3.0"
APP_PORT = 18080


def sh(*a, check=True):
    return subprocess.run(a, capture_output=True, text=True, check=check).stdout


def rm(*names):
    subprocess.run(["docker", "rm", "-f", *names], capture_output=True)


def build_image():
    sh("docker", "build", "-q", "-t", APP_IMAGE, os.path.join(HERE, "app-src"))


def start_infra():
    subprocess.run(["docker", "network", "create", NET], capture_output=True)
    rm("pg", "wiremock")
    sh("docker", "run", "-d", "--name", "pg", "--network", NET, "--cpus=2", "-m", "1g",
       "-e", "POSTGRES_USER=tx", "-e", "POSTGRES_PASSWORD=benchonly", "-e", "POSTGRES_DB=tx", PG_IMAGE)
    # Asynchronous responses, so the delay doesn't tie up WireMock's threads and WireMock never becomes the bottleneck.
    sh("docker", "run", "-d", "--name", "wiremock", "--network", NET, "--cpus=2", "-m", "1g",
       "-v", f"{os.path.join(HERE, 'wiremock')}:/home/wiremock:ro", WM_IMAGE,
       "--async-response-enabled=true", "--async-response-threads=50", "--container-threads=400", "--no-request-journal")
    for _ in range(120):
        if subprocess.run(["docker", "exec", "pg", "pg_isready", "-U", "tx"], capture_output=True).returncode == 0:
            break
        time.sleep(0.5)
    else:
        raise RuntimeError("PostgreSQL did not start")


def get(path, timeout=5):
    with urllib.request.urlopen(f"http://localhost:{APP_PORT}{path}", timeout=timeout) as r:
        return json.loads(r.read())


def start_app(fetch="eager", osiv=False, env=None):
    rm("app")
    extra = [x for k, v in (env or {}).items() for x in ("-e", f"{k}={v}")]
    sh("docker", "run", "-d", "--name", "app", "--network", NET, "--cpus=2", "-m", "1g", "-p", f"{APP_PORT}:8080",
       "-e", f"SPRING_DATASOURCE_CONNECTION_FETCH={fetch}", "-e", f"SPRING_JPA_OPEN_IN_VIEW={str(osiv).lower()}", *extra, APP_IMAGE)
    t0 = time.monotonic()
    while time.monotonic() - t0 < 120:
        try:
            if get("/actuator/health", timeout=1)["status"] == "UP":
                return
        except Exception:
            pass
        time.sleep(0.2)
    raise RuntimeError("app did not start:\n" + sh("docker", "logs", "app", check=False)[-3000:])


def metric(name):
    return get(f"/actuator/metrics/{name}", timeout=2)["measurements"][0]["value"]


def stop_all():
    rm("app", "pg", "wiremock")
