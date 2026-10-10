#!/usr/bin/env python3
import hashlib,json,subprocess,re,zipfile
from pathlib import Path
base=Path(__file__).resolve().parent
home=next((base/'toolchain').glob('graalvm-jdk-25*'))
def run(*args):
 p=subprocess.run(args,capture_output=True,text=True);return p.stdout+p.stderr
proof={'app_source_commits':{'current_frameworks':'3a29946','legacy':'fb1fb9c','shared_operations':'176a323'},'baseline_jvm_source_commit':'176a323','native_image_version':run(str(home/'bin/native-image'),'--version'),'gcc':run('gcc','--version'),'uname':run('uname','-a'),'compiler_options':['-J-Xmx8g','--parallelism=4','--no-fallback','-march=compatibility','-O2','--gc=serial'],'inputs':{}}
for group in ['standalone','quarkus','micronaut','spring','legacy']:
 directory=base/group
 proof['inputs'][group]={str(p.relative_to(directory)):{'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(directory.rglob('*')) if p.is_file() and not any(part.startswith(('aot-before','agent-config-incomplete')) for part in p.relative_to(directory).parts) and (p.suffix in ['.jar','.class','.json','.properties'] or p.name in ['native-image.args',group,'quarkus-probe-1-runner'])}
proof['cpu_model']=next(x for x in Path('/proc/cpuinfo').read_text().splitlines() if x.startswith('model name'))
proof['mem_total']=next(x for x in Path('/proc/meminfo').read_text().splitlines() if x.startswith('MemTotal:'))
proof['dynamic_linkage']={v:run('ldd',str(base/v/('quarkus-probe-1-runner' if v=='quarkus' else v))) for v in ['standalone','spring','legacy','quarkus','micronaut']}
proof['common_config']={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (base/'common-config').glob('*.json')}
proof['observed_reflection']={}
for v in ['standalone','spring','legacy','micronaut']:
 metadata=base/v/'agent-config/reachability-metadata.json'
 entries=json.loads(metadata.read_text())['reflection']
 names=[x['type'] for x in entries if isinstance(x['type'],str)]
 structured=[x['type'] for x in entries if isinstance(x['type'],dict)]
 proxies=[x['proxy'] for x in structured if 'proxy' in x]
 assert len(names)+len(structured)==len(entries)
 classic=base/v/'agent-config/reflect-config.json'
 proof['observed_reflection'][v]={'format':metadata.name,'total_entries':len(entries),'class_entries':len(names),'structured_types':structured,'proxy_interfaces':proxies,'retained_classic_entries':len(json.loads(classic.read_text())) if classic.exists() else 0,'teaql_package_types':[name for name in names if name.startswith('io.teaql.') and not name.startswith('io.teaql.benchmark.')],'application_package_types':[name for name in names if name.startswith('com.doublechaintech.liquidfilling.')]}
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
proof['standalone_input']='../../matched-six/modern/lib (see JVM environment.json hashes)'
proof['variant_options'] = {
 'legacy': ['--initialize-at-build-time=org.redisson.misc.BiHashMap,org.redisson.liveobject.core.RedissonObjectBuilder$CodecMethodRef'],
 'micronaut': ['-H:+SharedArenaSupport', '--initialize-at-run-time=io.netty,org.slf4j,ch.qos.logback',
               '--exclude-config netty-codec-http-.* /META-INF/native-image/io.netty/netty-codec-http/native-image\\.properties',
               '--exclude-config netty-codec-http2-.* /META-INF/native-image/io.netty/netty-codec-http2/native-image\\.properties'],
 'quarkus': ['--link-at-build-time=io.quarkus,io.teaql', '-H:+UseServiceLoaderFeature']
}
proof['metadata_training'] = 'Fresh GraalVM 25 agent output uses reachability-metadata.json. Classic baseline configuration is additionally retained for standalone and legacy. Counts describe fresh unified agent output, not all compiler reflection registrations.'
proof['script_sha256'] = {name: hashlib.sha256((base/name).read_bytes()).hexdigest()
                          for name in ['start-native.sh', 'build-native.sh']}
(base/'build-provenance.json').write_text(json.dumps(proof,indent=2)+'\n')
