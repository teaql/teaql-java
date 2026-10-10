#!/usr/bin/env python3
"""Blocked randomized fresh-native-process and bounded HTTP workload measurements."""
import argparse, concurrent.futures, csv, hashlib, json, math, os, random, re, signal, statistics, subprocess, time, urllib.request
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--runs',type=int,default=30);p.add_argument('--settle',type=float,default=10);p.add_argument('--load-repeats',type=int,default=5);p.add_argument('--seed',type=int,default=20261009);a=p.parse_args()
base=Path(__file__).resolve().parent;os.chdir(base)
variants=os.environ.get('NATIVE_VARIANTS','standalone,spring,quarkus,micronaut,legacy').split(',');rng=random.Random(a.seed)
out=base/('jvm-control-results-'+time.strftime('%Y%m%d-%H%M%S'));out.mkdir();rows=[];loads=[];active=None
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
        if not any(token in cmd for token in [str(base).encode()]):raise RuntimeError('Unexpected process')
        active.terminate();active.wait(timeout=20)
    active=None

def launch(v,log):
    global active
    env=dict(os.environ,BENCHMARK_VARIANT=v,NATIVE_JVM_CONTROL='true');start=time.monotonic_ns()
    active=subprocess.Popen(['bash','./start-native.sh'],env=env,stdout=log,stderr=subprocess.STDOUT,stdin=subprocess.DEVNULL)
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
meta={'runs_per_variant':a.runs,'settle_seconds':a.settle,'seed':a.seed,'load_repeats':a.load_repeats,'poll_interval_seconds':.02,'clock':'time.monotonic_ns','jvm_flags':['-Xms128m','-Xmx512m'],'jdk':'Amazon Corretto 21.0.12.12.1','platform':list(os.uname()),'expected_read':expected,'cache_policy':'Fresh JVM; OS cache not cleared; schema and fixtures outside timing.','libraries':{}}
meta['libraries']={str(p.relative_to(base/'micronaut')):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((base/'micronaut').rglob('*.jar'))}
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
