#!/usr/bin/env bash
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo"
disposable_evidence=""
if [[ -n "${TEAQL_COMMITTED_EVIDENCE_DIR:-}" ]]; then
  if [[ "$TEAQL_COMMITTED_EVIDENCE_DIR" != /* || -e "$TEAQL_COMMITTED_EVIDENCE_DIR" ]]; then
    echo 'Committed evidence directory must be an absent absolute path' >&2; exit 1
  fi
  evidence="$TEAQL_COMMITTED_EVIDENCE_DIR"; mkdir "$evidence"
else
  evidence="$(mktemp -d -t teaql-java-committed-generated.XXXXXXXX)"
  disposable_evidence="$evidence"
fi
cleanup() { status=$?; trap - EXIT; if [[ -n "$disposable_evidence" ]]; then rm -rf -- "$disposable_evidence"; fi; exit "$status"; }
trap cleanup EXIT
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
bash examples/committed-events/verify-security.sh
if [[ -n "${TEAQL_COMMITTED_EVIDENCE_DIR:-}" ]]; then
  echo "Java generated committed Q evidence: $evidence"
else
  echo 'Java generated committed Q disposable evidence removed on exit'
fi
