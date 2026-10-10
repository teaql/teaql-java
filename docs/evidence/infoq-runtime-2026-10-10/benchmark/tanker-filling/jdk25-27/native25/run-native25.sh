#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
while [ ! -f ../current-contract-post.json ]; do sleep 5; done
NATIVE_TRAIN=true NATIVE_VARIANTS=standalone,spring,micronaut,legacy python3 verify_contract.py > agent-training.log 2>&1
cp -a standalone/agent-config/. common-config/
python3 - <<'PY'
import json
from pathlib import Path
for p in Path('.').glob('*/agent-config/resource-config.json'):
 j=json.loads(p.read_text());j['resources']['includes']=[x for x in j['resources']['includes'] if not x['pattern'].startswith('\\Q/home/')];p.write_text(json.dumps(j,indent=2)+'\n')
p=Path('common-config/reflect-config.json');j=json.loads(p.read_text())
for entry in j:
 if entry.get('name')=='org.postgresql.Driver':entry['allDeclaredConstructors']=True
p.write_text(json.dumps(j,indent=2)+'\n')
PY
for v in standalone spring quarkus micronaut legacy; do
 echo "BUILD $v $(date -Is)"
 NATIVE_METADATA=true bash ./build-native.sh "$v"
done
python3 verify_contract.py
cp contract-verification.json contract-pre-campaign.json
python3 collect_native.py --runs 30 --load-repeats 5
python3 verify_contract.py
cp contract-verification.json contract-post-campaign.json
