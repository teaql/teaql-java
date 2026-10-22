#!/usr/bin/env bash
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
df -h "$repo" "${TMPDIR:-/tmp}"
scratch="$(mktemp -d -t teaql-java-cmr13.XXXXXXXX)"
task_pid=""
cleanup() {
  status=$?; trap - EXIT
  if [[ -n "$task_pid" ]]; then
    kill -TERM -- "-$task_pid" 2>/dev/null || true
    kill -KILL -- "-$task_pid" 2>/dev/null || true
    wait "$task_pid" 2>/dev/null || true
  fi
  rm -rf -- "$scratch"
  exit "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
command -v setsid >/dev/null
case_root="$scratch"
if [[ -n "${TEAQL_CMR13_EVIDENCE_ROOT:-}" ]]; then
  if [[ "$TEAQL_CMR13_EVIDENCE_ROOT" != /* || -e "$TEAQL_CMR13_EVIDENCE_ROOT" ]]; then
    echo 'CMR-13 evidence root must be an absent absolute path; never reset databases' >&2; exit 1
  fi
  mkdir "$TEAQL_CMR13_EVIDENCE_ROOT"
  case_root="$TEAQL_CMR13_EVIDENCE_ROOT"
fi
mkdir -p "$scratch/source" "$scratch/tmp"
tar -C "$repo" --exclude=.git --exclude=target -cf - . | tar -C "$scratch/source" -xf -
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.io.tmpdir=$scratch/tmp"
cd "$scratch/source"
for round in 1 2; do
  TEAQL_CMR13_EVIDENCE_DIR="$case_root/run-$round" setsid mvn -B -q -o \
    -pl examples/committed-events -am -Dtest=CommittedReceiverExampleTest \
    -Dsurefire.failIfNoSpecifiedTests=false test > "$scratch/round-$round.log" 2>&1 &
  task_pid=$!
  if wait "$task_pid"; then result=0; else result=$?; fi
  task_pid=""
  cat "$scratch/round-$round.log"
  if [[ -n "${TEAQL_CMR13_EVIDENCE_ROOT:-}" ]]; then
    cp "$scratch/round-$round.log" "$case_root/round-$round.log"
    reports=examples/committed-events/target/surefire-reports
    if [[ -e "$reports/TEST-io.teaql.examples.committed.CommittedReceiverExampleTest.xml" ]]; then
      cp "$reports/TEST-io.teaql.examples.committed.CommittedReceiverExampleTest.xml" "$case_root/round-$round.xml"
    fi
  fi
  if [[ "$result" != 0 ]]; then exit "$result"; fi
  rg -Fq 'CMR13_SECURITY_EXAMPLE_PASS language=java snapshot=1 tail=2 audited_saves=2 rejected=5 lost_ack=1 duplicate_no_rewind=1 reopen=1' "$scratch/round-$round.log"
  echo "PASS Java authenticated committed receiver example round $round"
done
