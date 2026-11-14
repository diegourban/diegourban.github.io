"""Shared setup for the MySQL lock benchmark: a MySQL 8.4 and a PostgreSQL 17 container, each with 2 CPUs and 1 GB,
and the booking app run as a local jar against one of them. The app connects as the database's admin user so the
deterministic check can see lock waits (information_schema.innodb_trx needs the PROCESS privilege)."""
import json, os, subprocess, time

HERE = os.path.dirname(os.path.abspath(__file__))
JAR = os.path.join(HERE, "app-src", "build", "libs", "bookingdemo-0.0.1-SNAPSHOT.jar")
MYSQL_IMAGE = "mysql:8.4.13"
PG_IMAGE = "postgres:17.6"
PASSWORD = "benchonly"
DBS = {
    "mysql": {"url": "jdbc:mysql://localhost:13306/booking", "user": "root"},
    "postgresql": {"url": "jdbc:postgresql://localhost:15432/booking", "user": "booking"},
}


def sh(*a, check=True):
    return subprocess.run(a, capture_output=True, text=True, check=check).stdout


def rm(*names):
    subprocess.run(["docker", "rm", "-f", *names], capture_output=True)


def build_jar():
    sh("gradle", "-q", "-p", os.path.join(HERE, "app-src"), "bootJar")


def start_dbs():
    rm("lockbench-mysql", "lockbench-pg")
    sh("docker", "run", "-d", "--name", "lockbench-mysql", "--cpus=2", "-m", "1g", "-p", "13306:3306",
       "-e", f"MYSQL_ROOT_PASSWORD={PASSWORD}", "-e", "MYSQL_DATABASE=booking", MYSQL_IMAGE)
    sh("docker", "run", "-d", "--name", "lockbench-pg", "--cpus=2", "-m", "1g", "-p", "15432:5432",
       "-e", "POSTGRES_USER=booking", "-e", f"POSTGRES_PASSWORD={PASSWORD}", "-e", "POSTGRES_DB=booking", PG_IMAGE)
    for _ in range(240):
        ok_my = subprocess.run(["docker", "exec", "lockbench-mysql", "mysql", "-uroot", f"-p{PASSWORD}", "-e", "select 1",
                                "booking"], capture_output=True).returncode == 0
        ok_pg = subprocess.run(["docker", "exec", "lockbench-pg", "pg_isready", "-U", "booking"],
                               capture_output=True).returncode == 0
        if ok_my and ok_pg:
            return
        time.sleep(0.5)
    raise RuntimeError("databases did not start")


def stop_dbs():
    rm("lockbench-mysql", "lockbench-pg")


def run_app(db, *args, isolation=None, timeout=3600):
    """Runs the jar once and returns its JSON lines. isolation sets Hikari's default for every connection."""
    cfg = DBS[db]
    cmd = ["java", "-jar", JAR, f"--spring.datasource.url={cfg['url']}", f"--spring.datasource.username={cfg['user']}",
           f"--spring.datasource.password={PASSWORD}", *args]
    if isolation:
        cmd.append(f"--spring.datasource.hikari.transaction-isolation={isolation}")
    p = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
    if p.returncode != 0:
        raise RuntimeError(f"app failed ({p.returncode}):\n{p.stdout[-2000:]}\n{p.stderr[-4000:]}")
    return [json.loads(line) for line in p.stdout.splitlines() if line.startswith("{")]
