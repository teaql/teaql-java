#!/usr/bin/env bash
set -euo pipefail
example="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
: "${TEAQL_MAVEN_DEPENDENCY_CACHE:?Use the documented isolated source qualifier for this gate}"
if [[ -n "${TEAQL_ORDER_EVIDENCE_DIR:-}" ]]; then
  evidence="$TEAQL_ORDER_EVIDENCE_DIR"
  mkdir "$evidence"
else
  evidence="$(mktemp -d -t teaql-java-order-evidence.XXXXXXXX)"
fi
scratch="$(mktemp -d -t teaql-java-order-build.XXXXXXXX)"
trap 'status=$?; trap - EXIT; rm -rf -- "$scratch"; exit "$status"' EXIT
fingerprint() {
  (cd "$example" && find pom.xml java-lib-core java-app-console -type f \
    ! -path '*/target/*' -print0 | sort -z | xargs -0 sha256sum)
}
fingerprint > "$evidence/source-before.sha256"
python3 -B - "$example" "$scratch" <<'PY'
from pathlib import Path
import shutil, sys
source, destination = map(Path, sys.argv[1:])
shutil.copyfile(source / 'pom.xml', destination / 'pom.xml')
for scope in ['java-lib-core', 'java-app-console']:
    for path in (source / scope).rglob('*'):
        assert not path.is_symlink(), path
    shutil.copytree(source / scope, destination / scope, ignore=shutil.ignore_patterns('target', '.git'))
PY
mvn -o -f "$scratch/pom.xml" install -DskipTests > "$evidence/consumer-build.log" 2>&1
mkdir "$evidence/consumer"
for round in 1 2; do
  (cd "$evidence/consumer" && timeout --kill-after=10s 180s \
    mvn -o -f "$scratch/pom.xml" exec:java -pl java-app-console) \
    > "$evidence/run-$round.log" 2>&1
done
fingerprint > "$evidence/source-after.sha256"
cmp "$evidence/source-before.sha256" "$evidence/source-after.sha256"
python3 -B - "$evidence" "$scratch" "$TEAQL_MAVEN_DEPENDENCY_CACHE" <<'PY'
import hashlib, json, pathlib, sqlite3, sys, xml.etree.ElementTree as ET
root, scratch, cache = map(pathlib.Path, sys.argv[1:])
for round in [1, 2]:
    text = (root / f'run-{round}.log').read_text()
    for marker in ['[schema] ensured 7 generated entity tables', '[query] matched 1 order(s)',
            'WEB-2026-001  2026-08-12  129.95',
            '[mutation-policy] denied high-value graph before persistence; verified 0 order rows',
            'BUILD SUCCESS']:
        assert marker in text, marker
    marker = ('[seed] inserted deterministic customer and order' if round == 1 else
        '[seed] deterministic data already exists; no duplicate rows added')
    assert marker in text, marker
pom = ET.parse(scratch / 'java-lib-core/lib/pom.xml').getroot()
namespace = {'m': 'http://maven.apache.org/POM/4.0.0'}
group, name, version = [pom.find('m:' + key, namespace).text for key in ['groupId', 'artifactId', 'version']]
jar = scratch / 'java-lib-core/lib/target' / f'{name}-{version}.jar'
installed = cache / group.replace('.', '/') / name / version / jar.name
assert jar.is_file() and installed.is_file() and jar.read_bytes() == installed.read_bytes()
database = root / 'consumer/.local/order.db'
with sqlite3.connect(database.resolve().as_uri() + '?mode=ro&immutable=1', uri=True) as connection:
    rows = connection.execute('SELECT order_number,order_date,total_amount,version FROM customer_order_data ORDER BY id').fetchall()
    assert len(rows) == 1 and rows[0][0:2] == ('WEB-2026-001', '2026-08-12'), rows
    assert str(rows[0][2]) == '129.95' and rows[0][3] == 1, rows
    presets = connection.execute('SELECT request_id,version FROM order_search_preset_data').fetchall()
    assert presets == [('quick-start-pending-orders', 1)], presets
    customers = connection.execute('SELECT name,version FROM customer_data').fetchall()
    assert customers == [('Acme Retail', 1)], customers
    assert connection.execute('SELECT count(*) FROM commerce_platform_data').fetchone() == (1,)
    assert connection.execute('PRAGMA quick_check').fetchone() == ('ok',)
receipt = dict(schema='teaql.java-order-business/v1', passed=True,
    originalBusinessSourcesUnchanged=True, nativeWorkspaceBridgeUsed=True,
    sameDatabaseStarts=2, generatedScopesUnchanged=True, deniedGraphHasNoOrderOrCustomer=True,
    artifactKind='new local source build; not a historical or published artifact',
    installedDomainArtifactSHA256=hashlib.sha256(jar.read_bytes()).hexdigest(),
    orders=rows, presets=presets, customers=customers,
    databaseSHA256=hashlib.sha256(database.read_bytes()).hexdigest())
(root / 'business-receipt.json').write_text(json.dumps(receipt, indent=2, sort_keys=True) + '\n')
print(json.dumps(dict(passed=True, sameDatabaseStarts=2, sourceArtifactInstalled=True)))
PY
