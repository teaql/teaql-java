#!/usr/bin/env python3
import hashlib,json,subprocess,re,zipfile
from pathlib import Path
base=Path(__file__).resolve().parent
home=next((base/'toolchain').glob('graalvm-jdk-21*'))
def run(*args):
 p=subprocess.run(args,capture_output=True,text=True);return p.stdout+p.stderr
proof={'app_source_commits':{'modern':'075990f','legacy':'fb1fb9c'},'baseline_jvm_source_commit':'176a323','native_image_version':run(str(home/'bin/native-image'),'--version'),'gcc':run('gcc','--version'),'uname':run('uname','-a'),'compiler_options':['-J-Xmx8g','--parallelism=4','--no-fallback','-march=compatibility'],'inputs':{}}
for group in ['standalone','quarkus','micronaut','spring','legacy']:
 directory=base/group
 proof['inputs'][group]={str(p.relative_to(directory)):{'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(directory.rglob('*')) if p.is_file() and not any(part.startswith(('aot-before','agent-config-incomplete')) for part in p.relative_to(directory).parts) and (p.suffix in ['.jar','.json','.properties'] or p.name in ['native-image.args',group,'quarkus-probe-1-runner'])}
proof['cpu_model']=next(x for x in Path('/proc/cpuinfo').read_text().splitlines() if x.startswith('model name'))
proof['mem_total']=next(x for x in Path('/proc/meminfo').read_text().splitlines() if x.startswith('MemTotal:'))
proof['dynamic_linkage']={v:run('ldd',str(base/v/('quarkus-probe-1-runner' if v=='quarkus' else v))) for v in ['standalone','spring','legacy','quarkus','micronaut']}
proof['common_config']={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (base/'common-config').glob('*.json')}
proof['observed_reflection']={}
for v in ['standalone','spring','legacy','micronaut']:
 entries=json.loads((base/v/'agent-config/reflect-config.json').read_text())
 proof['observed_reflection'][v]={'total_entries':len(entries),'teaql_package_types':[x['name'] for x in entries if x.get('name','').startswith('io.teaql.') and not x['name'].startswith('io.teaql.benchmark.')],'application_package_types':[x['name'] for x in entries if x.get('name','').startswith('com.doublechaintech.liquidfilling.')]}
proof['builds']={}
for v in ['standalone','spring','legacy','quarkus','micronaut']:
 directory=base/v
 proof['builds'][v]={}
 for log in directory.glob('build-*.log'):
  content=log.read_text();item={'sha256':hashlib.sha256(log.read_bytes()).hexdigest()}
  for key,pattern in [('elapsed',r'Elapsed \(wall clock\) time \(h:mm:ss or m:ss\): ([^\n]+)'),('peak_rss_kib',r'Maximum resident set size \(kbytes\): (\d+)'),('exit_status',r'Exit status: (\d+)')]:
   m=re.search(pattern,content)
   if m:item[key]=m.group(1)
  proof['builds'][v][log.name]=item
proof['standalone_input']='../matched-six/modern/lib (see JVM environment.json hashes)'
(base/'build-provenance.json').write_text(json.dumps(proof,indent=2)+'\n')
