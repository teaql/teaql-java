#!/usr/bin/env bash
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo"
evidence="$(mktemp -d -t teaql-java-committed-generated.XXXXXXXX)"
generated_library_hash() {
  (cd "$repo/examples/standard-order" && rg --files generated/lib -g '!target/**' \
    | LC_ALL=C sort | xargs -d '\n' sha256sum)
}
generated_library_hash > "$evidence/generated-before.sha256"
for round in 1 2; do
  mvn -B -q -pl examples/committed-events -am \
    -Dtest=CommittedEventsExampleTest -Dsurefire.failIfNoSpecifiedTests=false test \
    2>&1 | tee "$evidence/runtime-$round.log"
  rg -Fq 'COMMITTED_DELIVERY_RECOVERY_PASS language=java ordered=2 lost_ack=1 restart=1 duplicate=1' \
    "$evidence/runtime-$round.log"
  echo "PASS Java committed-events example round $round"
  mvn -B -q -Pruntime-examples -pl examples/standard-order -am \
    -Dtest=StandardOrderCommittedTargetTest -Dsurefire.failIfNoSpecifiedTests=false test \
    2>&1 | tee "$evidence/generated-$round.log"
  rg -Fq 'JAVA_COMMITTED_GENERATED_Q_PASS source=sqlite target=duckdb outbox=1' \
    "$evidence/generated-$round.log"
  generated_library_hash > "$evidence/generated-after.sha256"
  cmp "$evidence/generated-before.sha256" "$evidence/generated-after.sha256"
  echo "PASS Java generated committed Q example round $round"
done
bash examples/committed-events/verify-process-crash.sh
bash examples/committed-events/verify-snapshot.sh
bash examples/committed-events/verify-bootstrap-crash.sh
echo "Java generated committed Q evidence: $evidence"
