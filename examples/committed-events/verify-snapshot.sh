#!/usr/bin/env bash
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
df -h "$repo" "${TMPDIR:-/tmp}"
scratch="$(mktemp -d -t teaql-java-snapshot.XXXXXXXX)"
cleanup() { status=$?; trap - EXIT; rm -rf -- "$scratch"; exit "$status"; }
trap cleanup EXIT
mkdir -p "$scratch/source" "$scratch/tmp"
# Copy current local source, including edits, never a released dependency or
# stale HEAD. Exclude reproducible outputs, not generated source fixtures.
tar -C "$repo" --exclude=.git --exclude=target -cf - . | tar -C "$scratch/source" -xf -
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djava.io.tmpdir=$scratch/tmp"
cd "$scratch/source"
for round in 1 2; do
  mvn -B -q -o -pl examples/committed-events -am \
    -Dtest=CommittedSnapshotExampleTest -Dsurefire.failIfNoSpecifiedTests=false test \
    2>&1 | tee "$scratch/round-$round.log"
  rg -Fq 'CMR11_EXAMPLE_PASS language=java mode=local snapshot=0 tail=1 rows=2 catalog=2 duplicate_after_tail=1' "$scratch/round-$round.log"
  echo "PASS Java committed snapshot example round $round"
done
