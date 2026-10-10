#!/usr/bin/env python3
import hashlib,json,subprocess,zipfile
from pathlib import Path
b=Path(__file__).resolve().parent
proof={'app_source_commit':'3a29946','original_app_source':'176a323','toolchains':json.loads((b/'toolchain-provenance.json').read_text()),'native27':(b/'native27-availability.txt').read_text().strip(),'frameworks':{'original':{'spring':'3.2.0','quarkus':'3.8.3','micronaut_platform':'4.3.8','micronaut_core':'4.3.14'},'current':{'spring':'4.1.1','quarkus':'3.40.1','micronaut_platform':'5.2.2','micronaut_core':'5.2.15'}},'libraries':{}}
for group in ['legacy','modern','quarkus','micronaut','current-spring','current-quarkus','current-micronaut']:
 proof['libraries'][group]={str(p.relative_to(b/group)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((b/group).rglob('*.jar'))}
reference = proof['libraries']['modern']
shared = {Path(name).name: digest for name,digest in reference.items()
          if Path(name).name.startswith('teaql-') or Path(name).name == 'liquidfilling-core-297.jar'}
assert len(shared) == 9, shared
proof['shared_artifact_checks'] = {}
proof['business_class_sha256'] = {}
class_name = 'com/doublechaintech/liquidfilling/probe/ProbeOperations.class'
for group in ['modern','quarkus','micronaut','current-spring','current-quarkus','current-micronaut']:
    jars = proof['libraries'][group]
    for name,digest in shared.items():
        aliases = [name, 'liquidfilling-domain-297.jar'] if name == 'liquidfilling-core-297.jar' else [name]
        matches = [actual for path,actual in jars.items() if any(Path(path).name.endswith(alias) for alias in aliases)]
        assert matches == [digest], (group,name,matches)
    proof['shared_artifact_checks'][group] = list(shared)
    class_hashes = []
    for jar in (b/group).rglob('*.jar'):
        with zipfile.ZipFile(jar) as archive:
            if class_name in archive.namelist():
                class_hashes.append(hashlib.sha256(archive.read(class_name)).hexdigest())
    assert len(class_hashes) == 1, (group,class_hashes)
    proof['business_class_sha256'][group] = class_hashes[0]
assert len(set(proof['business_class_sha256'].values())) == 1
proof['vm_defaults']={}
proof['launcher_sha256'] = {name: hashlib.sha256((b/name).read_bytes()).hexdigest()
                            for name in ['start-jdk.sh', 'start-current.sh']}
for v in [25,27]:
 java=next((b/'toolchain').glob(f'jdk-{v}*/bin/java'));r=subprocess.run([str(java),'-XX:+PrintFlagsFinal','-version'],capture_output=True,text=True)
 proof['vm_defaults'][str(v)]=[line.strip() for line in r.stdout.splitlines() if any(name in line for name in ['UseG1GC','UseCompactObjectHeaders','AlwaysPreTouch','TieredCompilation'])]
(b/'build-provenance.json').write_text(json.dumps(proof,indent=2)+'\n')
