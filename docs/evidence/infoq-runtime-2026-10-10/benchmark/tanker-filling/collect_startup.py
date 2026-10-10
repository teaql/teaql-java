#!/usr/bin/env python3
"""Measure independent JVM launches against the isolated Spring baseline."""
import argparse
import csv
import hashlib
import json
import os
from pathlib import Path
import re
import signal
import statistics
import subprocess
import time
import urllib.request

parser = argparse.ArgumentParser()
parser.add_argument('--runs', type=int, default=30)
parser.add_argument('--settle-seconds', type=float, default=10)
parser.add_argument('--launcher', default='start-spring-baseline.sh')
parser.add_argument('--port', type=int, default=18880)
parser.add_argument('--label', default='spring-baseline')
parser.add_argument('--process-token', default='./app.jar')
parser.add_argument('--artifact', default='app.jar')
parser.add_argument('--java-path', default=str(Path.home() / 'teaql-startup-benchmark/runtime/amazon-corretto-17.0.20.12.1-linux-x64/bin/java'))
parser.add_argument('--ready-pattern', default=r'Started App in ([0-9.]+) seconds')
args = parser.parse_args()
base = Path(__file__).resolve().parent
os.chdir(base)
suffix = '' if args.label == 'spring-baseline' else '-' + args.label
out = base / ('results-' + time.strftime('%Y%m%d-%H%M%S') + suffix)
out.mkdir()
hz = os.sysconf('SC_CLK_TCK')
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def stop(pid):
    try:
        command = Path(f'/proc/{pid}/cmdline').read_bytes()
    except FileNotFoundError:
        return
    if args.process_token.encode() not in command:
        raise RuntimeError('Refusing to stop a process outside this benchmark')
    os.kill(pid, signal.SIGTERM)
    for _ in range(100):
        p = Path(f'/proc/{pid}/stat')
        if not p.exists() or p.read_text().split(') ', 1)[1].startswith('Z '):
            return
        time.sleep(.1)
    raise RuntimeError('Benchmark process did not stop gracefully')


def proc(pid):
    fields = Path(f'/proc/{pid}/stat').read_text().split(') ', 1)[1].split()
    status = {}
    for line in Path(f'/proc/{pid}/status').read_text().splitlines():
        k, _, v = line.partition(':')
        status[k] = v.strip()
    return {
        'rss_mib': int(status['VmRSS'].split()[0]) / 1024,
        'hwm_mib': int(status['VmHWM'].split()[0]) / 1024,
        'cpu_s': (int(fields[11]) + int(fields[12])) / hz,
        'threads': int(status['Threads']),
    }


def request(path, timeout=1):
    with opener.open(f'http://127.0.0.1:{args.port}' + path, timeout=timeout) as response:
        return json.load(response)


oldpid = base / 'app.pid'
if oldpid.exists():
    stop(int(oldpid.read_text()))
java = Path(args.java_path)
metadata = {
    'runs': args.runs, 'settle_seconds': args.settle_seconds, 'label': args.label,
    'launcher': args.launcher, 'port': args.port, 'mode': os.getenv('PROBE_MODE', 'spring'),
    'poll_interval_seconds': .05, 'clock': 'time.monotonic_ns',
    'jdk': subprocess.run([str(java), '-version'], capture_output=True, text=True).stderr.strip(),
    'platform': os.uname()._asdict() if hasattr(os.uname(), '_asdict') else list(os.uname()),
    'artifact': args.artifact,
    'artifact_sha256': hashlib.sha256((base / args.artifact).read_bytes()).hexdigest(),
    'dependency_jars': sorted(p.name for p in (base / 'lib').glob('*.jar')),
    'cpu_model': next((s.split(':', 1)[1].strip() for s in Path('/proc/cpuinfo').read_text().splitlines() if s.startswith('model name')), ''),
    'mem_total': next(s for s in Path('/proc/meminfo').read_text().splitlines() if s.startswith('MemTotal:')),
    'jvm_flags': ['-Xms128m', '-Xmx512m'],
    'conditions': 'Fresh JVM per run; OS cache not cleared; schema setup outside timed runs; PostgreSQL and Redis remain running; no JFR/NMT. Domain data state and endpoint scope are recorded in the accompanying report.',
    'web_ready_definition': 'Launcher ready marker observed and /version returns expected version JSON.',
    'query_definition': '/testS returns resultCode=0, status=YES and recordCount=0; exercises an empty-result database query, not full business workflow.',
}
(out / 'environment.json').write_text(json.dumps(metadata, indent=2) + '\n')
rows = []
active = None
try:
    for run in range(1, args.runs + 1):
        logfile = out / f'run-{run:02d}.log'
        with logfile.open('wb') as log:
            started = time.monotonic_ns()
            active = subprocess.Popen(['bash', './' + args.launcher], stdout=log, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL)
            oldpid.write_text(str(active.pid) + '\n')
            row = {'run': run, 'pid': active.pid}
            while True:
                if active.poll() is not None:
                    raise RuntimeError(f'Run {run} exited; see {logfile}')
                elapsed = (time.monotonic_ns() - started) / 1e9
                if elapsed > 60:
                    raise RuntimeError(f'Run {run} timed out')
                text = logfile.read_text(errors='replace')
                marker = re.search(args.ready_pattern, text)
                if marker:
                    try:
                        response = request('/version')
                        if response == {'version': 20240205}:
                            break
                    except Exception:
                        pass
                time.sleep(.05)
            row['web_ready_s'] = (time.monotonic_ns() - started) / 1e9
            startup_key = 'spring_logged_startup_s' if args.label == 'spring-baseline' else 'logged_startup_s'
            row[startup_key] = float(marker.group(1))
            snap = proc(active.pid)
            row.update({f'web_ready_{k}': v for k, v in snap.items()})
            query_start = time.monotonic_ns()
            response = request('/testS', timeout=30)
            query_end = time.monotonic_ns()
            if not (response.get('resultCode') == 0 and response.get('status') == 'YES' and response.get('recordCount') == 0):
                raise RuntimeError(f'Run {run} query failed validation')
            row['first_query_ms'] = (query_end - query_start) / 1e6
            row['query_ready_s'] = (query_end - started) / 1e9
            row.update({f'query_ready_{k}': v for k, v in proc(active.pid).items()})
            time.sleep(args.settle_seconds)
            row.update({f'settled_{k}': v for k, v in proc(active.pid).items()})
            rows.append(row)
            with (out / 'raw.csv').open('w', newline='') as f:
                writer = csv.DictWriter(f, fieldnames=list(rows[0]))
                writer.writeheader(); writer.writerows(rows)
            print(json.dumps({'run': run, 'web_ready_s': round(row['web_ready_s'], 3), 'query_ready_s': round(row['query_ready_s'], 3), 'ready_rss_mib': round(row['web_ready_rss_mib'], 1), 'settled_rss_mib': round(row['settled_rss_mib'], 1)}), flush=True)
        stop(active.pid)
        active.wait(timeout=10)
        active = None
    summary = {}
    for key in rows[0]:
        if key in ('run', 'pid'):
            continue
        values = sorted(r[key] for r in rows)
        # Nearest-rank percentile; 30 runs are insufficient for precise tail estimates.
        summary[key] = {'median': statistics.median(values), 'min': values[0], 'max': values[-1], 'p90': values[max(0, __import__('math').ceil(.9 * len(values)) - 1)]}
    (out / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
    print('RESULT_DIRECTORY=' + str(out), flush=True)
finally:
    if active is not None:
        stop(active.pid)
        active.wait(timeout=10)
    # Restore the deployed baseline after the collection campaign.
    with (base / 'app.log').open('ab') as log:
        restored = subprocess.Popen(['bash', './' + args.launcher], stdout=log, stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL, start_new_session=True)
        oldpid.write_text(str(restored.pid) + '\n')
