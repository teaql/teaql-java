#!/usr/bin/env bash
set -euo pipefail
example="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$example/../.." && pwd)"
scratch="$(mktemp -d -t teaql-java-reference.XXXXXXXX)"
trap 'status=$?; trap - EXIT; rm -rf -- "$scratch"; exit "$status"' EXIT
evidence="${TEAQL_REFERENCE_EVIDENCE_DIR:-$scratch/evidence}"
[[ "$evidence" == /* ]]
mkdir -- "$evidence"
export TEAQL_REFERENCE_DATABASE="$evidence/orders.sqlite"
python3 -B - "$scratch/pom.xml" "$repo" "$example/java-lib-core/lib" "$example/java-app-console" <<'PY'
import sys
from os.path import relpath
from pathlib import Path
from xml.etree.ElementTree import Element, ElementTree, SubElement

pom = Element('project', xmlns='http://maven.apache.org/POM/4.0.0')
for key, value in [('modelVersion','4.0.0'), ('groupId','io.teaql.test'), ('artifactId','order-editor-reactor'),
                   ('version','1'), ('packaging','pom')]:
    SubElement(pom, key).text = value
file = Path(sys.argv[1])
modules = SubElement(pom, 'modules')
for module in sys.argv[2:]:
    SubElement(modules, 'module').text = relpath(module, file.parent)
ElementTree(pom).write(file, encoding='utf-8', xml_declaration=True)
PY
cd "$repo"
mvn -q -pl examples/round-trip-reference-runtime -am \
  -Dtest=RoundTripReferenceRuntimeExampleTest,ContextBoundRoundTripReferenceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test >"$evidence/calibration.log" 2>&1 || {
    tail -80 "$evidence/calibration.log" >&2
    exit 1
  }
cp "$example/target/surefire-reports/TEST-io.teaql.examples.reference.RoundTripReferenceRuntimeExampleTest.xml" "$evidence/"
cp "$repo/teaql-runtime/target/surefire-reports/TEST-io.teaql.runtime.reference.ContextBoundRoundTripReferenceTest.xml" "$evidence/"
for round in 1 2; do
  TEAQL_SHOWCASE_ROUND="$round" mvn -q -f "$scratch/pom.xml" -pl :order-edit-reference -am \
    -Dtest=OrderReferenceEditorTest -Dsurefire.failIfNoSpecifiedTests=false test >"$evidence/round-$round.log" 2>&1 || {
      tail -80 "$evidence/round-$round.log" >&2
      exit 1
    }
  cp "$example/java-app-console/target/surefire-reports/TEST-com.teaql.ordereditreference.OrderReferenceEditorTest.xml" "$evidence/round-$round.xml"
  rg '^ORDER_REFERENCE ' "$evidence/round-$round.log"
done
