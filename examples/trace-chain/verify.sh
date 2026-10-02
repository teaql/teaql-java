#!/usr/bin/env bash
set -euo pipefail

example_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(cd "$example_dir/../.." && pwd)"
run_dir="${TEAQL_TRACE_CHAIN_VERIFY_DIR:-$(mktemp -d -t teaql-java-trace-chain.XXXXXXXX)}"
mkdir -p "$run_dir"
run_dir="$(cd "$run_dir" && pwd)"
database="$run_dir/trace-chain.db"

library_manifest() {
  (cd "$example_dir" && find lib -type f ! -path '*/target/*' -print0 |
    sort -z | xargs -0 sha256sum)
}
library_manifest > "$run_dir/library-before.sha256"

# Install only from this checkout. No candidate download, deploy, tag or publication.
mvn -B -f "$repo_dir/pom.xml" -Pruntime-examples -pl examples/trace-chain -am \
  install -DskipTests > "$run_dir/local-source-install.log" 2>&1

markers=(
  'PASS Java generated normative Trace Chain graph: six physical writes and committed audits'
  'PASS Java generated three-level SQL Trace Path and inherited request intent'
  'PASS Java generated Checker rejection before provider access'
  'PASS Java generated provider failure: attempted lineage, rollback, no committed audit'
  'PASS Java generated readback failure: separate outcomes and successful retry'
  'PASS Java generated prepared batch: per-item lineage and complete ledger replacement'
  'PASS Java generated prepared update/delete/recover: unequal versions and per-item lineage'
  'PASS Java generated overlapping Checker: valid commits, invalid rejected before provider'
  'PASS Java generated concurrent graphs: same Context, independent ledgers and per-item SQL/audit lineage'
  'PASS Java generated overlapping queries: request-owned three-level SQL paths, Context unchanged'
  'PASS Java generated stream: request-owned SQL path and delayed consumption intent'
)
for repetition in 1 2; do
  log="$run_dir/run-$repetition.log"
  if ! mvn -B -f "$example_dir/pom.xml" -Dtest=GeneratedTraceChainExampleTest \
    "-Dteaql.trace.database=$database" test > "$log" 2>&1; then
    tail -n 100 "$log" >&2
    exit 1
  fi
  for marker in "${markers[@]}"; do
    if ! grep -Fq "$marker" "$log"; then
      printf 'FAIL: missing acceptance marker: %s\n' "$marker" >&2
      exit 1
    fi
  done
  grep -Fq 'Tests run: 11, Failures: 0, Errors: 0, Skipped: 0' "$log"
  printf 'PASS Java generated Trace Chain run %s on the same database\n' "$repetition"
done
library_manifest > "$run_dir/library-after.sha256"
cmp "$run_dir/library-before.sha256" "$run_dir/library-after.sha256"
printf 'PASS Java generated library unchanged; evidence and database retained: %s\n' "$run_dir"
