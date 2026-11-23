#!/usr/bin/env bash
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo"
disposable_evidence=""
if [[ -n "${TEAQL_COMMITTED_EVIDENCE_DIR:-}" ]]; then
  if [[ "$TEAQL_COMMITTED_EVIDENCE_DIR" != /* || -e "$TEAQL_COMMITTED_EVIDENCE_DIR" ]]; then
    echo 'Committed evidence directory must be an absent absolute path' >&2; exit 1
  fi
  evidence="$TEAQL_COMMITTED_EVIDENCE_DIR"; mkdir "$evidence"
else
  evidence="$(mktemp -d -t teaql-java-committed-generated.XXXXXXXX)"
  disposable_evidence="$evidence"
fi
scratch=""
cleanup() {
  status=$?
  trap - EXIT
  [[ -z "$scratch" ]] || rm -rf -- "$scratch"
  [[ -z "$disposable_evidence" ]] || rm -rf -- "$disposable_evidence"
  exit "$status"
}
trap cleanup EXIT
scratch="$(mktemp -d -t teaql-java-committed-reactor.XXXXXXXX)"
python3 - "$repo" "$scratch" <<'PY_REACTOR'
from pathlib import Path
import os
import shutil
import sys
import xml.etree.ElementTree as ET

repo, scratch = map(Path, sys.argv[1:])
standard_order = repo / 'examples/standard-order'
library = scratch / 'library'
application = scratch / 'application'
shutil.copytree(standard_order / 'java-lib-core/lib', library, ignore=shutil.ignore_patterns('target'))
shutil.copytree(standard_order / 'java-app-console/src', application / 'src')

namespace = 'http://maven.apache.org/POM/4.0.0'
ET.register_namespace('', namespace)
def tag(name):
    return '{' + namespace + '}' + name

project = ET.parse(standard_order / 'java-app-console/pom.xml')
parent = project.getroot().find(tag('parent'))
parent.find(tag('relativePath')).text = os.path.relpath(repo / 'pom.xml', application)
project.write(application / 'pom.xml', encoding='utf-8', xml_declaration=True)

reactor = ET.Element(tag('project'))
for key, value in [('modelVersion', '4.0.0'), ('groupId', 'io.teaql.test'),
                   ('artifactId', 'committed-readback-reactor'), ('version', '1'), ('packaging', 'pom')]:
    ET.SubElement(reactor, tag(key)).text = value
modules = ET.SubElement(reactor, tag('modules'))
for module in (repo, library, application):
    ET.SubElement(modules, tag('module')).text = os.path.relpath(module, scratch)
ET.ElementTree(reactor).write(scratch / 'pom.xml', encoding='utf-8', xml_declaration=True)
PY_REACTOR
runtime_version="$(node - "$repo" "$scratch" <<'NODE'
const path = require('node:path');
const [repo, scratch] = process.argv.slice(2);
const {pin} = require(path.join(repo, 'scripts/pin-local-example-pom.cjs'));
console.log(pin(path.join(scratch, 'library/pom.xml'), repo, scratch).version);
NODE
)"
generated_library_hash() {
  (cd "$repo/examples/standard-order" && rg --files java-lib-core/lib -g '!target/**' \
    | LC_ALL=C sort | xargs -d '\n' sha256sum)
}
generated_library_hash > "$evidence/generated-before.sha256"
for round in 1 2; do
  mvn -B -q -pl examples/committed-events -am \
    -Dtest=CommittedEventsExampleTest -Dsurefire.failIfNoSpecifiedTests=false test \
    2>&1 | tee "$evidence/runtime-$round.log"
  rg -Fq 'COMMITTED_DELIVERY_RECOVERY_PASS language=java ordered=2 lost_ack=1 restart=1 duplicate=1' \
    "$evidence/runtime-$round.log"
  echo "PASS Java committed-events example round $round"
  mvn -B -q -f "$scratch/pom.xml" -pl :teaql-standard-order-example -am \
    "-Dteaql.version=$runtime_version" -Dtest=StandardOrderCommittedTargetTest -Dsurefire.failIfNoSpecifiedTests=false test \
    2>&1 | tee "$evidence/generated-$round.log"
  rg -Fq 'JAVA_COMMITTED_GENERATED_Q_PASS source=sqlite target=duckdb outbox=1' \
    "$evidence/generated-$round.log"
  generated_library_hash > "$evidence/generated-after.sha256"
  cmp "$evidence/generated-before.sha256" "$evidence/generated-after.sha256"
  echo "PASS Java generated committed Q example round $round"
done
bash examples/committed-events/verify-process-crash.sh
bash examples/committed-events/verify-snapshot.sh
bash examples/committed-events/verify-bootstrap-crash.sh
bash examples/committed-events/verify-security.sh
if [[ -n "${TEAQL_COMMITTED_EVIDENCE_DIR:-}" ]]; then
  echo "Java generated committed Q evidence: $evidence"
else
  echo 'Java generated committed Q disposable evidence removed on exit'
fi
