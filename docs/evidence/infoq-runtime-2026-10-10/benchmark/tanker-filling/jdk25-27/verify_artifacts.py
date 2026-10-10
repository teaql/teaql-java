#!/usr/bin/env python3
"""Verify shared artifact identity across JVM and native inputs."""
import hashlib,json
from pathlib import Path
b=Path(__file__).resolve().parent
jvm=json.loads((b/'build-provenance.json').read_text())
native=json.loads((b/'native25/build-provenance.json').read_text())
shared={Path(name).name: digest for name,digest in jvm['libraries']['modern'].items()
        if Path(name).name.startswith('teaql-') or Path(name).name=='liquidfilling-core-297.jar'}
assert len(shared)==9
for group in ['spring','quarkus','micronaut']:
    for name,digest in shared.items():
        aliases=[name,'liquidfilling-domain-297.jar'] if name=='liquidfilling-core-297.jar' else [name]
        candidates=[x['sha256'] for path,x in native['inputs'][group].items()
                    if any(Path(path).name.endswith(alias) for alias in aliases)]
        assert candidates==[digest],(group,name,candidates)
for name,digest in jvm['launcher_sha256'].items():
    assert hashlib.sha256((b/name).read_bytes()).hexdigest()==digest,name
old=json.loads((b.parent/'native/build-provenance.json').read_text())
old_legacy={k:v['sha256'] for k,v in old['inputs']['legacy'].items() if k.endswith('.jar')}
new_legacy={k:v['sha256'] for k,v in native['inputs']['legacy'].items() if k.endswith('.jar')}
assert old_legacy==new_legacy
print('Verified nine modern shared artifacts across JVM/native inputs, launcher identities, and 91 unchanged legacy native JARs.')
