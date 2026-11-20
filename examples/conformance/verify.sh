#!/usr/bin/env bash
set -euo pipefail
example="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$example/../.." && pwd)"
: "${TEAQL_MAVEN_DEPENDENCY_CACHE:?Use the isolated source qualifier for this gate}"
if [[ -n "${TEAQL_CONFORMANCE_EVIDENCE_DIR:-}" ]]; then
 evidence="$TEAQL_CONFORMANCE_EVIDENCE_DIR"
 mkdir "$evidence"
else
 evidence="$(mktemp -d -t teaql-java-work-item-evidence.XXXXXXXX)"
fi
scratch="$(mktemp -d -t teaql-java-work-item-build.XXXXXXXX)"
trap 'status=$?; trap - EXIT; rm -rf -- "$scratch"; exit "$status"' EXIT
fingerprint() {
 (cd "$example" && find java-lib-core java-app-console -type f ! -path '*/target/*' -print0 | sort -z | xargs -0 sha256sum)
}
fingerprint > "$evidence/source-before.sha256"
python3 -B - "$example" "$scratch" <<'CHECK'
from pathlib import Path
import shutil,sys
source,destination=map(Path,sys.argv[1:])
for scope in ['java-lib-core','java-app-console']:
 for path in (source/scope).rglob('*'):assert not path.is_symlink(),path
 shutil.copytree(source/scope,destination/scope,ignore=shutil.ignore_patterns('target','.git'))
CHECK
mvn -o -f "$scratch/java-lib-core/lib/pom.xml" install -DskipTests > "$evidence/library-build.log" 2>&1
for round in 1 2; do
 timeout --kill-after=10s 180s mvn -o -f "$scratch/java-app-console/pom.xml" spring-boot:run \
  "-Dspring-boot.run.arguments=--spring.main.web-application-type=none --server.port=0 --spring.datasource.url=jdbc:sqlite:$evidence/work-items.sqlite" > "$evidence/run-$round.log" 2>&1
 rg -Fq 'PASS Java minimum runtime conformance: 8/8' "$evidence/run-$round.log"
done
TEAQL_JAVA_SPRING_EVIDENCE_DIR="$evidence/spring-transactions" node "$example/spring-transactions/verify-local.cjs" > "$evidence/spring-transactions.log" 2>&1
fingerprint > "$evidence/source-after.sha256"
cmp "$evidence/source-before.sha256" "$evidence/source-after.sha256"
python3 -B - "$evidence" "$scratch" "$TEAQL_MAVEN_DEPENDENCY_CACHE" <<'CHECK'
import hashlib,json,pathlib,sqlite3,sys,xml.etree.ElementTree as ET
root,scratch,cache=map(pathlib.Path,sys.argv[1:]);spring=json.loads((root/'spring-transactions/receipt.json').read_text())
assert spring['passed'] and spring['cleanup']['scratchRemoved'] and len(spring['rounds'])==2
assert all(len(row['observations'])==8 for row in spring['rounds'])
for round in [1,2]:
 log=(root/f'run-{round}.log').read_text();assert 'PASS Java minimum runtime conformance: 8/8' in log and 'BUILD SUCCESS' in log
pom=ET.parse(scratch/'java-lib-core/lib/pom.xml').getroot();namespace={'m':'http://maven.apache.org/POM/4.0.0'}
group,name,version=[pom.find('m:'+key,namespace).text for key in ['groupId','artifactId','version']]
jar=scratch/'java-lib-core/lib/target'/f'{name}-{version}.jar';installed=cache/group.replace('.','/')/name/version/jar.name
assert jar.is_file() and installed.is_file() and jar.read_bytes()==installed.read_bytes()
database=root/'work-items.sqlite'
with sqlite3.connect(database.resolve().as_uri()+'?mode=ro&immutable=1',uri=True) as connection:
 rows=connection.execute('SELECT title,version FROM work_item_data ORDER BY id').fetchall();assert len(rows)==2 and all(row[1]==-3 for row in rows),rows
 assert connection.execute('PRAGMA quick_check').fetchone()==('ok',)
receipt=dict(schema='teaql.java-work-item-business/v1',passed=True,originalBusinessAndSpringTestSourcesByteExact=True,
 starts=2,sameDatabase=True,databaseNeverReset=True,springBusinessCasesPerRound=8,springRounds=2,
 nativeWorkspaceEntryUsed=True,generatedScopesUnchanged=True,artifactKind='new local source build; not a historical or published artifact',
 installedDomainArtifactSHA256=hashlib.sha256(jar.read_bytes()).hexdigest(),workItems=rows,
 springReceiptSHA256=hashlib.sha256((root/'spring-transactions/receipt.json').read_bytes()).hexdigest(),
 databaseSHA256=hashlib.sha256(database.read_bytes()).hexdigest())
(root/'business-receipt.json').write_text(json.dumps(receipt,indent=2,sort_keys=True)+'\n')
print(json.dumps(dict(passed=True,starts=2,springCasesPerRound=8,springRounds=2)))
CHECK
