#!/usr/bin/env bash
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
df -h "$repo" "${TMPDIR:-/tmp}"
scratch="$(mktemp -d -t teaql-java-bootstrap-crash.XXXXXXXX)"
maven_pid=""
stop_owned_build() {
  if [[ -n "$maven_pid" ]]; then
    # setsid gives this build (and its worker JVMs) its own process group.
    kill -TERM -- "-$maven_pid" 2>/dev/null || true
    kill -KILL -- "-$maven_pid" 2>/dev/null || true
    wait "$maven_pid" 2>/dev/null || true
    maven_pid=""
  fi
}
cleanup() { status=$?; trap - EXIT; stop_owned_build; rm -rf -- "$scratch"; exit "$status"; }
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
command -v setsid >/dev/null
mkdir -p "$scratch/source" "$scratch/tmp"
# Local repository code only. Compatible compilations are reused for both runs.
tar -C "$repo" --exclude=.git --exclude=target -cf - . | tar -C "$scratch/source" -xf -
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.io.tmpdir=$scratch/tmp"
if [[ -n "${TEAQL_CMR12_EVIDENCE_ROOT:-}" ]]; then
  if [[ "$TEAQL_CMR12_EVIDENCE_ROOT" != /* ]]; then
    echo 'CMR-12 evidence root must be absolute' >&2
    exit 1
  fi
  if [[ -e "$TEAQL_CMR12_EVIDENCE_ROOT" ]]; then
    echo 'CMR-12 evidence root must be fresh; never reset prior databases' >&2
    exit 1
  fi
  mkdir -p "$TEAQL_CMR12_EVIDENCE_ROOT"
fi
cd "$scratch/source"
for run in 1 2; do
  if [[ -n "${TEAQL_CMR12_EVIDENCE_ROOT:-}" ]]; then
    export TEAQL_CMR12_EVIDENCE_DIR="$TEAQL_CMR12_EVIDENCE_ROOT/run-$run"
  else
    unset TEAQL_CMR12_EVIDENCE_DIR
  fi
  setsid mvn -B -q -o -pl examples/committed-events -am \
    -Dtest=CommittedBootstrapCrashExampleTest -Dsurefire.failIfNoSpecifiedTests=false \
    "-Dcmr12.source.root=$scratch/source" test > "$scratch/run-$run.log" 2>&1 &
  maven_pid=$!
  if wait "$maven_pid"; then result=0; else result=$?; fi
  stop_owned_build
  cat "$scratch/run-$run.log"
  if [[ "$result" != 0 ]]; then exit "$result"; fi
  for round in 1 2; do
    rg -Fq "CMR12_PROCESS_PASS language=java round=$round baseline=$((2 * round - 1)) tail=$((2 * round)) killed=7 same_source_database=1 original_bytes=1" "$scratch/run-$run.log"
  done
  if [[ -n "${TEAQL_CMR12_EVIDENCE_ROOT:-}" ]]; then
    mkdir -p "$TEAQL_CMR12_EVIDENCE_ROOT/run-$run/junit"
    cp examples/committed-events/target/surefire-reports/TEST-io.teaql.examples.committed.CommittedBootstrapCrashExampleTest.xml \
      "$TEAQL_CMR12_EVIDENCE_ROOT/run-$run/junit/"
  fi
  echo "PASS Java managed bootstrap process example run $run rounds=2 killed=14"
done
