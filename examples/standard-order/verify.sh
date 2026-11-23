#!/usr/bin/env bash
set -euo pipefail
example="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$example/../.." && pwd)"
evidence="${TEAQL_JAVA_STANDARD_ORDER_EVIDENCE_DIR:-}"
if [[ -n "$evidence" ]]; then
  mkdir "$evidence"
else
  evidence="$(mktemp -d -t teaql-java-standard-order-evidence.XXXXXXXX)"
fi
mode="${1:-}"
rounds=(first second)
if [[ "$mode" == --capture ]]; then
  [[ "${TEAQL_TELEMETRY_ENABLED:-}" == true ]]
  [[ "${TEAQL_TELEMETRY_OTLP_ENDPOINT:-}" =~ ^http://127\.0\.0\.1:[0-9]+$ ]]
  rounds=(enabled)
else
  [[ -z "$mode" ]]
  unset TEAQL_TELEMETRY_ENABLED TEAQL_TELEMETRY_SERVICE_NAME TEAQL_TELEMETRY_OTLP_ENDPOINT
fi
hash_library() {
  (cd "$example" && find java-lib-core -type f \
    ! -path '*/target/*' -print0 | sort -z | xargs -0 sha256sum)
}
hash_library > "$evidence/generated-before.sha256"
for round in "${rounds[@]}"; do
  mkdir "$evidence/$round"
  TEAQL_JAVA_STANDARD_ORDER_DATABASE_DIR="$evidence/$round" \
    timeout --kill-after=10s 180s mvn -q -f "$repo/pom.xml" -Pruntime-examples \
    -pl examples/standard-order/java-app-console -am -Dtest=StandardOrderTelemetryTest,StandardOrderGraphStreamTelemetryTest \
    -Dsurefire.failIfNoSpecifiedTests=false test 2>&1 | tee "$evidence/$round.log"
  rg -Fq 'JAVA_STANDARD_ORDER PASSED' "$evidence/$round.log"
  rg -Fq 'JAVA_STANDARD_GRAPH PASSED' "$evidence/$round.log"
  rg -Fq 'JAVA_STANDARD_STREAM PASSED' "$evidence/$round.log"
  for name in TEAQL_TELEMETRY_ENABLED TEAQL_TELEMETRY_SERVICE_NAME TEAQL_TELEMETRY_OTLP_ENDPOINT; do
    rg -Fq "$name" "$evidence/$round.log"
  done
  if [[ "$round" == enabled ]]; then
    rg -Fq 'TeaQL telemetry: enabled; full recording' "$evidence/$round.log"
  else
    rg -Fq 'TeaQL telemetry: supported, disabled' "$evidence/$round.log"
  fi
  cp "$example/java-app-console/target/surefire-reports"/TEST-*StandardOrder*Test.xml "$evidence/$round"
done
hash_library > "$evidence/generated-after.sha256"
cmp "$evidence/generated-before.sha256" "$evidence/generated-after.sha256"
echo "PASS Java standard Order example; evidence retained: $evidence"
