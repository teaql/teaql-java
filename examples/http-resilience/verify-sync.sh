#!/usr/bin/env bash
set -euo pipefail
example="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$example/../.." && pwd)"
scratch="$(mktemp -d -t teaql-java-supplier-sync.XXXXXXXX)"
trap 'status=$?; trap - EXIT; rm -rf -- "$scratch"; exit "$status"' EXIT
evidence="${TEAQL_SUPPLIER_SYNC_EVIDENCE_DIR:-$scratch/evidence}"
[[ "$evidence" == /* ]]
mkdir -- "$evidence"
export TEAQL_SUPPLIER_SYNC_DATABASE="$evidence/supplier-orders.sqlite"
python3 -B - "$scratch/pom.xml" "$repo" "$example/java-lib-core/lib" "$example/java-app-console" <<'PY'
import sys
from os.path import relpath
from pathlib import Path
from xml.etree.ElementTree import Element, ElementTree, SubElement

project = Element('project', xmlns='http://maven.apache.org/POM/4.0.0')
properties = [('modelVersion', '4.0.0'), ('groupId', 'io.teaql.test'),
              ('artifactId', 'supplier-sync-reactor'), ('version', '1'), ('packaging', 'pom')]
for key, value in properties:
    SubElement(project, key).text = value
file = Path(sys.argv[1])
modules = SubElement(project, 'modules')
for module in sys.argv[2:]:
    SubElement(modules, 'module').text = relpath(module, file.parent)
ElementTree(project).write(file, encoding='utf-8', xml_declaration=True)
PY
for round in 1 2; do
  TEAQL_SHOWCASE_ROUND="$round" mvn -q -f "$scratch/pom.xml" -pl :supplier-order-sync-service -am \
    -Dtest=SupplierOrderSyncTest -Dsurefire.failIfNoSpecifiedTests=false test >"$evidence/round-$round.log" 2>&1 || {
    tail -80 "$evidence/round-$round.log" >&2
    exit 1
  }
  cp "$example/java-app-console/target/surefire-reports/TEST-com.teaql.supplierordersyncservice.SupplierOrderSyncTest.xml" "$evidence/round-$round.xml"
  rg '^SUPPLIER_ORDER_SYNC=' "$evidence/round-$round.log"
done
