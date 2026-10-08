#!/usr/bin/env bash
set -euo pipefail

repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
verification_dir="$(mktemp -d)"
trap 'rm -rf -- "$verification_dir"' EXIT
expected=(business-id-runtime conformance order-management round-trip-reference-runtime school-management security-foundations shared-load-state trace-chain)
mapfile -t actual < <(find "$repo/examples" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort)
if [[ "${actual[*]}" != "${expected[*]}" ]]; then
  echo "example inventory changed; update scripts/verify-examples.sh: ${actual[*]}" >&2
  exit 1
fi

cd "$repo"
mvn -q -DskipTests install
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
echo "PASS: standard Java examples; shared-load-state has its own generator-backed verify.sh gate"
