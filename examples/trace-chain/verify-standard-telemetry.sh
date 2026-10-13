#!/usr/bin/env bash
set -euo pipefail

example_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(cd "$example_dir/../.." && pwd)"
run_dir="$(mktemp -d -t teaql-java-standard-telemetry.XXXXXXXX)"
names=(TEAQL_TELEMETRY_ENABLED TEAQL_TELEMETRY_SERVICE_NAME TEAQL_TELEMETRY_OTLP_ENDPOINT)
disabled=(env -u "${names[0]}" -u "${names[1]}" -u "${names[2]}")
args=(-B -f "$repo_dir/pom.xml" -Pruntime-examples -pl examples/trace-chain -am
  -Dtest=GeneratedStandardTelemetryExampleTest -Dsurefire.failIfNoSpecifiedTests=false test)

library_manifest() {
  (cd "$example_dir" && find lib -type f ! -path '*/target/*' -print0 |
    sort -z | xargs -0 sha256sum)
}
library_manifest > "$run_dir/library-before.sha256"
for repetition in 1 2; do
  "${disabled[@]}" mvn "${args[@]}" > "$run_dir/disabled-$repetition.log" 2>&1
  grep -Fq 'JAVA_STANDARD_TELEMETRY_EXAMPLE PASSED' "$run_dir/disabled-$repetition.log"
  grep -Fq 'TeaQL telemetry: supported, disabled' "$run_dir/disabled-$repetition.log"
  for name in "${names[@]}"; do grep -Fq "$name" "$run_dir/disabled-$repetition.log"; done
done
if [[ ${TEAQL_TELEMETRY_ENABLED:-false} == true ]]; then
  for repetition in 1 2; do
    mvn "${args[@]}" > "$run_dir/enabled-$repetition.log" 2>&1
    grep -Fq 'JAVA_STANDARD_TELEMETRY_EXAMPLE PASSED' "$run_dir/enabled-$repetition.log"
    grep -Fq 'TeaQL telemetry: enabled' "$run_dir/enabled-$repetition.log"
  done
fi
library_manifest > "$run_dir/library-after.sha256"
cmp "$run_dir/library-before.sha256" "$run_dir/library-after.sha256"
printf 'PASS Java standard telemetry example; evidence=%s\n' "$run_dir"
