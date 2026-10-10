#!/usr/bin/env python3
"""Preflight all variants; schema initialization is outside the measured campaign."""
import json,os,subprocess,time,urllib.request,urllib.error
from pathlib import Path
base=Path(__file__).resolve().parent;os.chdir(base)
expected={'name':'Benchmark merchant','expressionName':'Benchmark merchant','platformId':1,'taskLifeHours':48}
proof={}
def req(path):
 with urllib.request.build_opener(urllib.request.ProxyHandler({})).open('http://127.0.0.1:18884'+path,timeout=30) as r:return json.load(r)
for variant in ['legacy-spring','modern-spring','modern-classpath','modern-jpms','modern-quarkus','modern-micronaut']:
 with (base/(variant+'-contract.log')).open('wb') as log:
  p=subprocess.Popen(['bash','./start-matched.sh'],env=dict(os.environ,BENCHMARK_VARIANT=variant,BENCHMARK_ENSURE_TABLE='true'),stdout=log,stderr=subprocess.STDOUT)
  try:
   deadline=time.monotonic()+90
   while True:
    if p.poll() is not None:raise RuntimeError(variant+' exited')
    try:
     if req('/version')=={'version':20240205}:break
    except Exception:pass
    if time.monotonic()>deadline:raise RuntimeError(variant+' timed out')
    time.sleep(.05)
   steps=[]
   for path,value in [('/ensureDB',{'ok':True}),('/ensureDB',{'ok':True}),('/setup',expected),('/read',expected),('/write?hours=49',dict(expected,taskLifeHours=49)),('/write?hours=48',expected),('/testS',{'data':[],'resultCode':0,'status':'YES','recordCount':0})]:
    got=req(path);assert got==value,(variant,path,got);steps.append({'path':path,'response':got})
   try:
    req('/write?hours=47')
    raise AssertionError('Out-of-contract update was accepted')
   except urllib.error.HTTPError as failure:
    assert failure.code==500,failure.code
    steps.append({'path':'/write?hours=47','status':failure.code})
   got=req('/read');assert got==expected;steps.append({'path':'/read after rejected write','response':got})
   proof[variant]=steps;print(variant+' contract passed',flush=True)
  finally:
   if p.poll() is None:p.terminate();p.wait(timeout=20)
(base/'contract-verification.json').write_text(json.dumps(proof,indent=2)+'\n')
