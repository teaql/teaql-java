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
runtime_version="$(node -e 'const fs=require("fs");process.stdout.write(fs.readFileSync(process.argv[1],"utf8").match(/<version>([^<]+)<\/version>/)[1])' "$repo/pom.xml")"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Dmaven.repo.local=$build_dir/maven -Dteaql.version=$runtime_version"
echo "Native example artifacts use an isolated disposable Maven repository; compact evidence: $evidence_dir"
expected=(business-id-runtime conformance exact-decimal fix-e-alignment http-resilience inheritance-save object-storage order-management round-trip-reference-runtime scalar-type-probe school-management security-foundations shared-load-state standard-order trace-chain)
mapfile -t actual < <(find "$repo/examples" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort)
if [[ "${actual[*]}" != "${expected[*]}" ]]; then
  echo "example inventory changed; update scripts/verify-examples.sh: ${actual[*]}" >&2
  exit 1
fi

cd "$repo"
# This verifier needs original Git provenance and already creates its own
# disposable source-paired reactor. It never builds in the source repository.
node "${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}/examples/fix-e-alignment/verify-local.cjs"
node "${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}/examples/inheritance-save/verify.cjs" \
  --output "$evidence_dir/inheritance-save"
bash examples/object-storage/verify.sh
bash examples/http-resilience/verify.sh
TEAQL_SUPPLIER_SYNC_EVIDENCE_DIR="$evidence_dir/supplier-order-sync" bash examples/http-resilience/verify-sync.sh
mvn -q -DskipTests install
mvn -q -pl teaql-sqlite \
  -Dtest=DerivedQueryTraceSqliteTest,LikeIntentPrivacySqliteTest,TypedIntentPrivacySqliteTest test
mvn -q -pl examples/business-id-runtime \
  -Dtest=BusinessIdRuntimeExampleTest test
mvn -q -o -f examples/business-id-runtime/java-lib-core/lib/pom.xml -DskipTests install
TEAQL_BUSINESS_ID_KEY_V1_HEX=000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f \
TEAQL_PURCHASE_NUMBERING_ROUND=inventory \
mvn -q -o -f examples/business-id-runtime/java-app-console/pom.xml compile \
  org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=com.teaql.purchaseordernumbering.App \
  "-Dexec.args=--teaql.datasource.url=jdbc:sqlite:$evidence_dir/business-id.sqlite"
mvn -q -pl examples/security-foundations \
  -Dtest=SecurityFoundationsExampleTest,SqlRelationMaskingTest test
TEAQL_REFERENCE_EVIDENCE_DIR="$evidence_dir/round-trip-reference" bash examples/round-trip-reference-runtime/verify.sh
TEAQL_CONFORMANCE_EVIDENCE_DIR="$evidence_dir/conformance" bash examples/conformance/verify.sh
mvn -q -f examples/school-management/java-lib-core/lib/pom.xml install -DskipTests
mvn -q -f examples/school-management/java-app-console/pom.xml spring-boot:run \
  -Dspring-boot.run.arguments="--spring.main.web-application-type=none --spring.datasource.url=jdbc:sqlite:$verification_dir/school-management.db"
TEAQL_ORDER_EVIDENCE_DIR="$evidence_dir/order-management" bash examples/order-management/verify.sh
bash examples/trace-chain/verify.sh
bash examples/trace-chain/verify-standard-telemetry.sh
TEAQL_JAVA_STANDARD_ORDER_EVIDENCE_DIR="$verification_dir/standard-order" bash examples/standard-order/verify.sh
TEAQL_CODEGEN_DIR="$build_dir/generator" bash examples/shared-load-state/verify.sh --generate
TEAQL_LOAD_STATE_EVIDENCE_DIR="$evidence_dir/shared-load-state" bash examples/shared-load-state/verify.sh --workspace
TEAQL_MAINSTREAM_EVIDENCE_DIR="$evidence_dir/mainstream-reference" bash examples/shared-load-state/benchmark-mainstream.sh --workspace
TEAQL_JAVA_EXACT_EVIDENCE_DIR="$evidence_dir/exact-decimal" \
  bash "${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}/examples/exact-decimal/verify.sh"
TEAQL_FREIGHT_QUOTE_EVIDENCE_DIR="$evidence_dir/freight-quotes" bash "${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}/examples/scalar-type-probe/verify-freight-quotes.sh"
node "$repo/examples/conformance/context-derived/verify-local.cjs"
TEAQL_NATURAL_MESSAGES_EVIDENCE_DIR="$evidence_dir/natural-messages" \
  node "${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}/examples/conformance/natural-messages/verify-local.cjs"
echo "PASS: all Java examples"
