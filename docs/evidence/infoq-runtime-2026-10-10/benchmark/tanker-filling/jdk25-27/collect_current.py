#!/usr/bin/env python3
"""Blocked randomized fresh-JVM and bounded HTTP workload measurements."""
import argparse, concurrent.futures, csv, hashlib, json, math, os, random, re, signal, statistics, subprocess, time, urllib.request
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--runs',type=int,default=30);p.add_argument('--settle',type=float,default=10);p.add_argument('--load-repeats',type=int,default=5);p.add_argument('--seed',type=int,default=20261009);a=p.parse_args()
base=Path(__file__).resolve().parent;os.chdir(base)
variants=[f'current-{v}-jdk{jdk}' for jdk in [25,27] for v in ['spring','quarkus','micronaut']];rng=random.Random(a.seed)
out=base/('current-results-'+time.strftime('%Y%m%d-%H%M%S'));out.mkdir();rows=[];loads=[];active=None
opener=urllib.request.build_opener(urllib.request.ProxyHandler({}));hz=os.sysconf('SC_CLK_TCK')
expected={'name':'Benchmark merchant','expressionName':'Benchmark merchant','platformId':1,'taskLifeHours':48}
def req(path,expected_value=None):
    # Separate opener per request avoids shared urllib state during read concurrency.
    client=urllib.request.build_opener(urllib.request.ProxyHandler({}))
    with client.open('http://127.0.0.1:18884'+path,timeout=30) as f: value=json.load(f)
    if expected_value is not None and value!=expected_value:raise RuntimeError(f'{path} unexpected response: {value}')
    return value

def snap(pid):
    stat=Path(f'/proc/{pid}/stat').read_text().split(') ',1)[1].split();s={}
    for line in Path(f'/proc/{pid}/status').read_text().splitlines():k,_,v=line.partition(':');s[k]=v.strip()
    return dict(rss_mib=int(s['VmRSS'].split()[0])/1024,hwm_mib=int(s['VmHWM'].split()[0])/1024,cpu_s=(int(stat[11])+int(stat[12]))/hz,threads=int(s['Threads']))

def stop():
    global active
    if active is None:return
    if active.poll() is None:
        cmd=Path(f'/proc/{active.pid}/cmdline').read_bytes()
        if not any(token in cmd for token in [b'liquidfilling.probe.',b'liquidfilling.legacyprobe.',b'quarkus/quarkus-run.jar',b'io.teaql.benchmark.micronaut.Application',b'current-quarkus/quarkus-run.jar']):raise RuntimeError('Unexpected process')
        active.terminate();active.wait(timeout=20)
    active=None

def launch(v,log):
    global active
    env=dict(os.environ,BENCHMARK_VARIANT=v);start=time.monotonic_ns()
    active=subprocess.Popen(['bash','./start-current.sh'],env=env,stdout=log,stderr=subprocess.STDOUT,stdin=subprocess.DEVNULL)
    while True:
        if active.poll() is not None:raise RuntimeError(f'{v} exited')
        if (time.monotonic_ns()-start)/1e9>90:raise RuntimeError(f'{v} timed out')
        try:
            if req('/version')=={'version':20240205}:break
        except Exception:pass
        time.sleep(.02)
    return start

def persist(name,data):
    with (out/name).open('w',newline='') as f:w=csv.DictWriter(f,fieldnames=list(data[0]));w.writeheader();w.writerows(data)

def summarize(data):
    result={}
    for v in variants:
        subset=[r for r in data if r['variant']==v];metrics={}
        for k in subset[0]:
            if k in ['variant','block','pid','order']:continue
            vals=sorted(r[k] for r in subset)
            metrics[k]={'median':statistics.median(vals),'min':vals[0],'max':vals[-1],'p90':vals[math.ceil(.9*len(vals))-1]}
        result[v]=metrics
    return result
java=next((base/'toolchain').glob('jdk-25*/bin/java'))
meta={'runs_per_variant':a.runs,'settle_seconds':a.settle,'seed':a.seed,'load_repeats':a.load_repeats,'poll_interval_seconds':.02,'clock':'time.monotonic_ns','jdk':subprocess.run([str(java),'-version'],capture_output=True,text=True).stderr,'jvm_flags':['-Xms128m','-Xmx512m','-Dlogging.config='+str(base/'logback.xml'),'-Dlogback.configurationFile='+str(base/'logback.xml')], 'logging':'Identical explicit WARN console configuration; no file appender' ,'platform':list(os.uname()),'cpu_model':next(x for x in Path('/proc/cpuinfo').read_text().splitlines() if x.startswith('model name')),'mem_total':next(x for x in Path('/proc/meminfo').read_text().splitlines() if x.startswith('MemTotal:')),'expected_read':expected,'matrix':'Current frameworks; wrappers compiledJava25 with same shared business/domain JARs; runtime25/27','cache_policy':'Fresh JVM; OS cache not cleared; schema and fixtures outside timing; no Redis use, no JFR/NMT.','frameworks':{'spring_boot':'4.1.1','quarkus':'3.40.1','micronaut_platform':'5.2.2','micronaut_core':'5.2.15'},'libraries':{}}
for group in ['current-spring','current-quarkus','current-micronaut']:
 meta['libraries'][group]={str(j.relative_to(base/group)):hashlib.sha256(j.read_bytes()).hexdigest() for j in sorted((base/group).rglob('*.jar'))}
(out/'environment.json').write_text(json.dumps(meta,indent=2)+'\n')
try:
 for block in range(1,a.runs+1):
    order=variants.copy();rng.shuffle(order)
    for position,v in enumerate(order):
        with (out/f'startup-{block:02d}-{v}.log').open('wb') as log:
            start=launch(v,log);row=dict(block=block,order=position,variant=v,pid=active.pid,web_ready_s=(time.monotonic_ns()-start)/1e9)
            row.update({'web_ready_'+k:x for k,x in snap(active.pid).items()})
            t=time.monotonic_ns();req('/read',expected);end=time.monotonic_ns();row.update(first_read_ms=(end-t)/1e6,business_ready_s=(end-start)/1e9)
            row.update({'business_ready_'+k:x for k,x in snap(active.pid).items()})
            for hours in [49,48]:
                t=time.monotonic_ns();req('/write?hours='+str(hours),dict(expected,taskLifeHours=hours));row['first_write_'+str(hours)+'_ms']=(time.monotonic_ns()-t)/1e6
            req('/testS',{'data':[],'resultCode':0,'status':'YES','recordCount':0})
            time.sleep(a.settle);row.update({'settled_'+k:x for k,x in snap(active.pid).items()})
            rows.append(row);persist('startup.csv',rows);print(json.dumps(row),flush=True)
        stop()
 (out/'startup-summary.json').write_text(json.dumps(summarize(rows),indent=2)+'\n')
 # Separate workload campaign: identical bounded warmup and measurement for every fresh JVM.
 for block in range(1,a.load_repeats+1):
    order=variants.copy();rng.shuffle(order)
    for position,v in enumerate(order):
        with (out/f'load-{block:02d}-{v}.log').open('wb') as log:
            launch(v,log)
            for _ in range(200):req('/read',expected)
            for _ in range(10):req('/write?hours=49',dict(expected,taskLifeHours=49));req('/write?hours=48',expected)
            lat=[]
            def read_one(_):
                t=time.monotonic_ns();req('/read',expected);return (time.monotonic_ns()-t)/1e6
            before=snap(active.pid);t=time.monotonic_ns()
            with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:lat=list(pool.map(read_one,range(400)))
            seconds=(time.monotonic_ns()-t)/1e9;after=snap(active.pid);writes=[];t=time.monotonic_ns()
            for _ in range(20):
                for hours in [49,48]:
                    begin=time.monotonic_ns();req('/write?hours='+str(hours),dict(expected,taskLifeHours=hours));writes.append((time.monotonic_ns()-begin)/1e6)
            write_seconds=(time.monotonic_ns()-t)/1e9;last=snap(active.pid)
            row=dict(block=block,order=position,variant=v,pid=active.pid,read_requests=400,read_concurrency=4,read_seconds=seconds,read_rps=400/seconds,read_median_ms=statistics.median(lat),read_p95_ms=sorted(lat)[379],read_cpu_s=after['cpu_s']-before['cpu_s'],write_requests=40,write_concurrency=1,write_seconds=write_seconds,write_rps=40/write_seconds,write_median_ms=statistics.median(writes),write_p95_ms=sorted(writes)[37],write_cpu_s=last['cpu_s']-after['cpu_s'],rss_mib=last['rss_mib'],hwm_mib=last['hwm_mib'])
            loads.append(row);persist('load.csv',loads)
            (out/f'latency-{block:02d}-{v}.json').write_text(json.dumps({'read_ms':lat,'write_ms':writes})+'\n');print('LOAD '+json.dumps(row),flush=True)
        stop()
 (out/'load-summary.json').write_text(json.dumps(summarize(loads),indent=2)+'\n')
 print('RESULT_DIRECTORY='+str(out),flush=True)
finally:stop()
