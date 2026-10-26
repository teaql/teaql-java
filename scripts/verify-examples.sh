#!/usr/bin/env bash
set -euo pipefail

repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
verification_dir="$(mktemp -d)"
trap 'rm -rf -- "$verification_dir"' EXIT
expected=(blob-view-preparation business-id-runtime conformance http-resilience object-storage order-management round-trip-reference-runtime school-management security-foundations shared-load-state standard-order trace-chain view-preparation)
mapfile -t actual < <(find "$repo/examples" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort)
if [[ "${actual[*]}" != "${expected[*]}" ]]; then
  echo "example inventory changed; update scripts/verify-examples.sh: ${actual[*]}" >&2
  exit 1
fi

cd "$repo"
bash examples/object-storage/verify.sh
bash examples/http-resilience/verify.sh
mvn -q -DskipTests install
mvn -q -pl examples/view-preparation exec:java \
  -Dexec.mainClass=io.teaql.examples.view.BaseViewExample
if [[ -n "${TEAQL_BLOB_TEST_PG_URL:-}" && -n "${TEAQL_BLOB_TEST_PG_USER:-}" && -n "${TEAQL_BLOB_TEST_PG_PASSWORD:-}" && -n "${TEAQL_VIEW_PDF:-}" ]]; then
  mvn -q -pl examples/blob-view-preparation exec:java \
    -Dexec.mainClass=io.teaql.examples.blobview.BlobViewExample
else
  echo "PASS: Blob view example compiled; standalone run requires an isolated PostgreSQL peer and TEAQL_VIEW_PDF"
fi
mvn -q -pl teaql-sqlite \
  -Dtest=DerivedQueryTraceSqliteTest,LikeIntentPrivacySqliteTest,TypedIntentPrivacySqliteTest test
mvn -q -pl examples/business-id-runtime \
  -Dtest=BusinessIdRuntimeExampleTest test
mvn -q -pl examples/security-foundations \
  -Dtest=SecurityFoundationsExampleTest,SqlRelationMaskingTest test
mvn -q -pl examples/round-trip-reference-runtime \
  -Dtest=RoundTripReferenceRuntimeExampleTest test
mvn -q -f examples/conformance/lib/pom.xml install -DskipTests
mvn -q -f examples/conformance/pom.xml spring-boot:run \
  -Dspring-boot.run.arguments="--spring.main.web-application-type=none --spring.datasource.url=jdbc:sqlite:$verification_dir/conformance.db"
mvn -q -f examples/school-management/lib/pom.xml install -DskipTests
mvn -q -f examples/school-management/pom.xml spring-boot:run \
  -Dspring-boot.run.arguments="--spring.main.web-application-type=none --spring.datasource.url=jdbc:sqlite:$verification_dir/school-management.db"
mvn -q -f examples/order-management/pom.xml install -DskipTests
# The console resolves .local/order.db from its process directory. Never mutate
# the developer's retained database while verifying the runtime checkout.
(cd "$verification_dir" && mvn -q -f "$repo/examples/order-management/pom.xml" exec:java -pl java-app-console)
bash examples/trace-chain/verify.sh
bash examples/trace-chain/verify-standard-telemetry.sh
bash examples/standard-order/verify.sh
echo "PASS: standard Java examples; shared-load-state has its own generator-backed verify.sh gate"
