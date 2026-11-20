#!/usr/bin/env bash
set -euo pipefail
example="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$example/../.." && pwd)"
scratch="$(mktemp -d -t teaql-java-merchant-privacy.XXXXXXXX)"
trap 'status=$?; trap - EXIT; rm -rf -- "$scratch"; exit "$status"' EXIT
evidence="${TEAQL_ORDER_PRIVACY_EVIDENCE_DIR:-$scratch/evidence}"
[[ "$evidence" == /* ]]
mkdir -- "$evidence"
export TEAQL_ORDER_PRIVACY_DATABASE="$evidence/orders.sqlite"
python3 -B - "$scratch/pom.xml" "$repo" "$example/java-lib-core/lib" "$example/java-app-console" <<'PY'
import sys
from os.path import relpath
from pathlib import Path
from xml.etree.ElementTree import Element, ElementTree, SubElement

project = Element('project', xmlns='http://maven.apache.org/POM/4.0.0')
properties = [('modelVersion', '4.0.0'), ('groupId', 'io.teaql.test'),
              ('artifactId', 'merchant-privacy-reactor'), ('version', '1'), ('packaging', 'pom')]
for key, value in properties:
    SubElement(project, key).text = value
file = Path(sys.argv[1])
modules = SubElement(project, 'modules')
for module in sys.argv[2:]:
    SubElement(modules, 'module').text = relpath(module, file.parent)
ElementTree(project).write(file, encoding='utf-8', xml_declaration=True)
PY
unset TEAQL_ALLOW_SENSITIVE_PLAINTEXT_LOGS TEAQL_SQL_DEBUG_ENDPOINT TEAQL_AUDIT_DEBUG_ENDPOINT
export TEAQL_LOG_FORMAT=json TEAQL_TELEMETRY_ENABLED=false
for round in 1 2; do
  if [[ "$round" == 2 ]]; then
    export TEAQL_ALLOW_SENSITIVE_PLAINTEXT_LOGS=I_UNDERSTAND_SENSITIVE_DATA_MAY_BE_WRITTEN_TO_DISK
  fi
  TEAQL_SHOWCASE_ROUND="$round" TEAQL_LOG_ENDPOINT="$evidence/ordinary-$round.log" \
    TEAQL_ORDER_PRIVACY_DEBUG_FILE="$evidence/debug-$round.log" mvn -q -f "$scratch/pom.xml" -pl :merchant-order-privacy-service -am \
    -Dtest=MerchantOrderPrivacyTest -Dsurefire.failIfNoSpecifiedTests=false test >"$evidence/round-$round.log" 2>&1 || {
    tail -80 "$evidence/round-$round.log" >&2
    exit 1
  }
  cp "$example/java-app-console/target/surefire-reports/TEST-com.teaql.merchantorderprivacyservice.MerchantOrderPrivacyTest.xml" "$evidence/round-$round.xml"
  rg '^ORDER_PRIVACY=' "$evidence/round-$round.log"
done
