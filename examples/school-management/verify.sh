#!/usr/bin/env bash
set -euo pipefail
example="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$example/../.." && pwd)"
: "${TEAQL_MAVEN_DEPENDENCY_CACHE:?Use the documented isolated source qualifier for this gate}"
if [[ -n "${TEAQL_SCHOOL_EVIDENCE_DIR:-}" ]]; then
  evidence="$TEAQL_SCHOOL_EVIDENCE_DIR"
  mkdir "$evidence"
else
  evidence="$(mktemp -d -t teaql-java-school-evidence.XXXXXXXX)"
fi
scratch="$(mktemp -d -t teaql-java-school-build.XXXXXXXX)"
trap 'status=$?; trap - EXIT; rm -rf -- "$scratch"; exit "$status"' EXIT
fingerprint() {
  (cd "$example" && find java-lib-core java-app-console -type f \
    ! -path '*/target/*' -print0 | sort -z | xargs -0 sha256sum)
}
fingerprint > "$evidence/source-before.sha256"
python3 -B - "$example" "$scratch" <<'PY'
from pathlib import Path
import shutil, sys
source, destination = map(Path, sys.argv[1:])
for scope in ['java-lib-core', 'java-app-console']:
    for path in (source / scope).rglob('*'):
        assert not path.is_symlink(), path
    shutil.copytree(source / scope, destination / scope, ignore=shutil.ignore_patterns('target', '.git'))
PY
library="$scratch/java-lib-core/lib/pom.xml"
workspace="$scratch/java-app-console/pom.xml"
mvn -o -f "$library" install -DskipTests > "$evidence/library-build.log" 2>&1
for round in 1 2; do
  timeout --kill-after=10s 180s mvn -o -f "$workspace" spring-boot:run \
    "-Dspring-boot.run.arguments=--spring.main.web-application-type=none --server.port=0 --spring.datasource.url=jdbc:sqlite:$evidence/showcase.sqlite" \
    > "$evidence/run-$round.log" 2>&1
done
fingerprint > "$evidence/source-after.sha256"
cmp "$evidence/source-before.sha256" "$evidence/source-after.sha256"
python3 -B - "$evidence" "$scratch" "$TEAQL_MAVEN_DEPENDENCY_CACHE" <<'PY'
import hashlib, json, pathlib, sqlite3, sys, xml.etree.ElementTree as ET
root, scratch, cache = map(pathlib.Path, sys.argv[1:])
markers = ['PASS Java generated School dynamic search: related scope, drift warnings, typed bindings',
    'PASS Java School Q/E/Checker/audited create-update-delete lifecycle',
    'PASS Java request-owned comment/purpose gates and generated relation inheritance',
    'PASS Java School bootstrap, portable Query, and native SQLite Facet parity']
for round in [1, 2]:
    text = (root / f'run-{round}.log').read_text()
    for marker in markers:
        assert marker in text, marker
    state = f'fresh={str(round == 1).lower()} originalVersion={1 if round == 1 else 3}'
    assert 'PASS Java generated bootstrap request intent, committed trace and reconciliation ' + state in text
    assert 'BUILD SUCCESS' in text
pom = ET.parse(scratch / 'java-lib-core/lib/pom.xml').getroot()
namespace = {'m': 'http://maven.apache.org/POM/4.0.0'}
group, name, version = [pom.find('m:' + key, namespace).text for key in ['groupId', 'artifactId', 'version']]
jar = scratch / 'java-lib-core/lib/target' / f'{name}-{version}.jar'
installed = cache / group.replace('.', '/') / name / version / jar.name
assert jar.is_file() and installed.is_file() and jar.read_bytes() == installed.read_bytes()
database = root / 'showcase.sqlite'
with sqlite3.connect(database.resolve().as_uri() + '?mode=ro&immutable=1', uri=True) as connection:
    rows = connection.execute('SELECT name,version,student_capacity FROM school_data WHERE version>0').fetchall()
    assert rows == [('Riverside Primary School', 1, 800)], rows
    deleted = connection.execute('SELECT name,version FROM school_data WHERE version<0').fetchall()
    assert deleted == [('Lifecycle Probe Academy', -3), ('Lifecycle Probe Academy', -3)], deleted
    types = connection.execute('SELECT id,version FROM school_type_data ORDER BY id').fetchall()
    assert types == [(1001, 5), (1002, 1)], types
    assert connection.execute('SELECT count(*) FROM platform_data').fetchone() == (1,)
    assert connection.execute('PRAGMA quick_check').fetchone() == ('ok',)
receipt = dict(schema='teaql.java-school-business/v1', passed=True, originalApplicationAssertionsPassed=True,
    sameDatabaseStarts=2, generatedScopesUnchanged=True, originalApplicationSourceUnchanged=True,
    artifactKind='new local source build; not a historical or published artifact',
    installedDomainArtifactSHA256=hashlib.sha256(jar.read_bytes()).hexdigest(),
    sqliteRows=rows, deletedLifecycleRows=deleted, schoolTypeVersions=types,
    databaseSHA256=hashlib.sha256(database.read_bytes()).hexdigest())
(root / 'business-receipt.json').write_text(json.dumps(receipt, indent=2, sort_keys=True) + '\n')
print(json.dumps(dict(passed=True, sameDatabaseStarts=2, sourceArtifactInstalled=True)))
PY
