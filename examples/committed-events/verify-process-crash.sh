#!/usr/bin/env bash
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo"
# No copied build tree: Maven uses this checkout's stable module targets.
# The small, owned fixture DBs/logs are retained and their path is printed.
# TEAQL_CMR03_EVIDENCE_DIR, when supplied, must be a fresh evidence directory.
output="$(mvn -B -q -pl examples/committed-events -am \
  -Dtest=CommittedProcessCrashExampleTest -Dsurefire.failIfNoSpecifiedTests=false test 2>&1)" || {
  printf '%s\n' "$output" >&2
  exit 1
}
printf '%s\n' "$output"
for round in 1 2; do
  if ! rg -q "^CMR03_PROCESS_CRASH_PASS language=java round=$round " <<< "$output"; then
    echo "missing Java real-process crash round $round" >&2
    exit 1
  fi
done
echo 'PASS Java CMR-03 real source-process crash rounds=2 same_databases=1'
