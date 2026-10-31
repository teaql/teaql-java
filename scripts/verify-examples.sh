#!/usr/bin/env bash
set -euo pipefail

repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
evidence_dir="${TEAQL_EXAMPLES_EVIDENCE_DIR:-$(mktemp -d -t teaql-java-examples-evidence.XXXXXXXX)}"
verification_dir="$evidence_dir/runtime"
mkdir -p "$verification_dir"
build_dir="$(mktemp -d -t teaql-java-examples-build.XXXXXXXX)"
cleanup() { result=$?; trap - EXIT; wait; rm -rf -- "$build_dir"; exit "$result"; }
trap cleanup EXIT
: "${TEAQL_CODEGEN_DIR:?Set TEAQL_CODEGEN_DIR to the local generator checkout}"
node "$repo/scripts/prepare-example-isolation.cjs" "$build_dir" "$TEAQL_CODEGEN_DIR" "$evidence_dir"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Dmaven.repo.local=$build_dir/maven"
echo "Native example artifacts use an isolated disposable Maven repository; compact evidence: $evidence_dir"
expected=(business-id-runtime conformance exact-decimal fix-e-alignment http-resilience object-storage order-management round-trip-reference-runtime scalar-type-probe school-management security-foundations shared-load-state standard-order trace-chain)
mapfile -t actual < <(find "$repo/examples" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort)
if [[ "${actual[*]}" != "${expected[*]}" ]]; then
  echo "example inventory changed; update scripts/verify-examples.sh: ${actual[*]}" >&2
  exit 1
fi

cd "$repo"
# This verifier needs original Git provenance and already creates its own
# disposable source-paired reactor. It never builds in the source repository.
node "${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}/examples/fix-e-alignment/verify-local.cjs"
bash examples/object-storage/verify.sh
bash examples/http-resilience/verify.sh
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
bash examples/trace-chain/verify-standard-telemetry.sh
bash examples/standard-order/verify.sh
TEAQL_CODEGEN_DIR="$build_dir/generator" bash examples/shared-load-state/verify.sh --generate
TEAQL_JAVA_EXACT_EVIDENCE_DIR="$evidence_dir/exact-decimal" \
  bash "${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}/examples/exact-decimal/verify.sh"
TEAQL_JAVA_SCALAR_EVIDENCE_DIR="$evidence_dir/scalar-type-probe" \
  node "${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}/examples/scalar-type-probe/verify-local.cjs"
TEAQL_JAVA_SCALAR_EVIDENCE_DIR="$evidence_dir/restoration-views" \
  node "${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}/examples/scalar-type-probe/verify-local.cjs" --restoration-views
TEAQL_JAVA_JSON_PROFILE_EVIDENCE_DIR="$evidence_dir/json-profiles" \
  node "${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}/examples/scalar-type-probe/verify-json-profiles.cjs"
TEAQL_JAVA_JSON_PROFILE_EVIDENCE_DIR="$evidence_dir/json-lifecycle" \
  node "${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}/examples/scalar-type-probe/verify-json-profiles.cjs" --lifecycle
TEAQL_JAVA_JSON_PROFILE_EVIDENCE_DIR="$evidence_dir/json-numeric" \
  node "${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}/examples/scalar-type-probe/verify-json-profiles.cjs" --numeric
TEAQL_JAVA_JSON_PROFILE_EVIDENCE_DIR="$evidence_dir/json-temporal" \
  node "${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}/examples/scalar-type-probe/verify-json-profiles.cjs" --temporal
TEAQL_JAVA_JSON_PROFILE_EVIDENCE_DIR="$evidence_dir/rejected-mutation" \
  node "${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}/examples/scalar-type-probe/verify-json-profiles.cjs" --rejected
TEAQL_JAVA_JSON_PROFILE_EVIDENCE_DIR="$evidence_dir/explicit-null" \
  node "${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}/examples/scalar-type-probe/verify-json-profiles.cjs" --explicit-null
TEAQL_JAVA_JSON_PROFILE_EVIDENCE_DIR="$evidence_dir/loaded-revert" \
  node "${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}/examples/scalar-type-probe/verify-json-profiles.cjs" --loaded-revert
TEAQL_JAVA_JSON_PROFILE_EVIDENCE_DIR="$evidence_dir/version-lifecycle" \
  node "${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}/examples/scalar-type-probe/verify-json-profiles.cjs" --version-lifecycle
echo "PASS: all Java examples"
