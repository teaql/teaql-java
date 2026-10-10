#!/usr/bin/env python3
"""Audit completed retained observations and deployed artifact identity."""
import argparse,csv,json,math,statistics,re,hashlib
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('results',type=Path);a=p.parse_args();r=a.results;base=Path(__file__).resolve().parent
for csvname,summaryname,n in [('startup.csv','startup-summary.json',30),('load.csv','load-summary.json',5)]:
 rows=list(csv.DictReader((r/csvname).open()));summary=json.loads((r/summaryname).read_text());assert len(rows)==len(summary)*n
 for block in range(1,n+1):assert {x['variant'] for x in rows if int(x['block'])==block}==set(summary)
 for v,metrics in summary.items():
  rr=[x for x in rows if x['variant']==v];assert len(rr)==n
  for key,s in metrics.items():
   vals=sorted(float(x[key]) for x in rr);assert abs(statistics.median(vals)-s['median'])<1e-8 and abs(vals[math.ceil(.9*n)-1]-s['p90'])<1e-8
  if csvname=='load.csv':
   for row in rr:
    b=int(row['block']);d=json.loads((r/f'latency-{b:02d}-{v}.json').read_text())
    for prefix,count in [('read',400),('write',40)]:
     vals=sorted(d[prefix+'_ms']);assert len(vals)==count
     assert abs(statistics.median(vals)-float(row[prefix+'_median_ms']))<1e-8
     assert abs(vals[math.ceil(.95*count)-1]-float(row[prefix+'_p95_ms']))<1e-8
     assert abs(count/float(row[prefix+'_seconds'])-float(row[prefix+'_rps']))<1e-8
variants=set(json.loads((r/'startup-summary.json').read_text()));assert len(list(r.glob('startup-*.log')))==len(variants)*30 and len(list(r.glob('load-*.log')))==len(variants)*5
for f in r.glob('*.log'):
 s=f.read_text();assert not re.search(r'ERROR|Exception|\[TeaQL SQL\]',s),f.name
env=json.loads((r/'environment.json').read_text());assert env['jvm_flags']==['-Xms128m','-Xmx512m'];assert variants=={'micronaut'}
prov=json.loads((base/'build-provenance.json').read_text())
assert env['libraries']=={name:item['sha256'] for name,item in prov['inputs']['micronaut'].items() if name.endswith('.jar')}
(base/'control-result-checksums.json').write_text(json.dumps({f.name:hashlib.sha256(f.read_bytes()).hexdigest() for f in sorted(r.iterdir()) if f.is_file()},indent=2)+'\n')
print(f'Verified {len(variants)*30} startup rows, {len(variants)*5} load rows; medians/p90/p95/rates and clean timed logs, and exact native-compatible JAR hashes.')
