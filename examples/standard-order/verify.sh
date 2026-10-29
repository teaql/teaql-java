#!/usr/bin/env bash
set -euo pipefail
example="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$example/../.." && pwd)"
evidence="$(mktemp -d "${TMPDIR:-/tmp}/teaql-java-standard-order-evidence-XXXXXX")"
hash_library() {
  (cd "$example" && find generated/lib -type f ! -path '*/target/*' -print0 | sort -z | xargs -0 sha256sum)
}
hash_library > "$evidence/generated-before.sha256"
for round in first second; do
  timeout --kill-after=10s 180s env -u TEAQL_TELEMETRY_ENABLED -u TEAQL_TELEMETRY_SERVICE_NAME \
    -u TEAQL_TELEMETRY_OTLP_ENDPOINT mvn -q -f "$repo/pom.xml" -Pruntime-examples \
    -pl examples/standard-order -am -Dtest=StandardOrderTelemetryTest,StandardOrderGraphStreamTelemetryTest \
    -Dsurefire.failIfNoSpecifiedTests=false test 2>&1 | tee "$evidence/$round.log"
  rg -Fq 'JAVA_STANDARD_ORDER PASSED' "$evidence/$round.log"
  rg -Fq 'JAVA_STANDARD_GRAPH PASSED' "$evidence/$round.log"
  rg -Fq 'JAVA_STANDARD_STREAM PASSED' "$evidence/$round.log"
  for name in TEAQL_TELEMETRY_ENABLED TEAQL_TELEMETRY_SERVICE_NAME TEAQL_TELEMETRY_OTLP_ENDPOINT; do
    rg -Fq "$name" "$evidence/$round.log"
  done
done
hash_library > "$evidence/generated-after.sha256"
cmp "$evidence/generated-before.sha256" "$evidence/generated-after.sha256"
echo "PASS Java standard Order example; evidence retained: $evidence"
