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
  'TC-REQ-09 JAVA GENERATED BOOTSTRAP PASSED logging=true'
  'TC-REQ-09 JAVA GENERATED BOOTSTRAP PASSED logging=false'
  'PASS Java generated privacy retains complete raw command and safe SQL/audit root lineage'
  'PASS FORWARD_NOTLOADED: Java generated Q/E retains FK and hidden detail guard'
  'PASS Java generated cursor evidence: logging disabled, completion, cancellation and failure'
  'PASS Java generated query evidence: logging disabled, three relation levels, immutable result list'
  'PASS Java generated cross-type loaded privacy: repeated saves, rollback retry, delete and independent intent'
  'PASS Java generated normative Trace Chain graph: six physical writes and committed audits'
  'PASS Java graph identity controls: duplicate, missing and equal-ID type collapse rejected'
  'PASS Java generated three-level SQL Trace Path and inherited request intent'
  'PASS Java generated Checker rejection before provider access'
  'PASS Java generated provider failure: attempted lineage, rollback, no committed audit'
  'PASS Java generated readback failure: separate outcomes and successful retry'
  'PASS Java generated prepared batch: per-item lineage and complete ledger replacement'
  'PASS Java generated ledger override: one typed key replaces fallback; new sibling inherits only graph root at command/SQL/audit'
  'PASS Java generated prepared update/delete/recover: unequal versions and per-item lineage'
  'PASS Java generated overlapping Checker: valid commits, invalid rejected before provider'
  'PASS Java generated concurrent graphs: same Context, independent ledgers and per-item SQL/audit lineage'
  'PASS Java generated overlapping queries: request-owned three-level SQL paths, Context unchanged'
  'PASS Java generated stream: request-owned SQL path and delayed consumption intent'
  'PASS Java generated nested facets: filtered counts and original root/relation SQL paths'
  'PASS Java generated relation facet: original root and complete inherited SQL route'
)
for repetition in 1 2; do
  log="$run_dir/run-$repetition.log"
  if ! mvn -B -f "$example_dir/pom.xml" -Dtest=GeneratedTraceChainExampleTest,GeneratedFacetTraceExampleTest,GeneratedReverseFacetTraceExampleTest,GeneratedAggregateTraceExampleTest \
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
  grep -Fq 'Tests run: 24, Failures: 0, Errors: 0, Skipped: 0' "$log"
  for aggregate_marker in JAVA_AGGREGATE_OBSERVED JAVA_AGGREGATE_NUMERIC JAVA_AGGREGATE_MEMBERSHIP JAVA_AGGREGATE_FORWARD; do
    expected_count=4
    if [[ $aggregate_marker == JAVA_AGGREGATE_MEMBERSHIP ]]; then expected_count=8; fi
    [[ $(grep -c "^$aggregate_marker " "$log") == "$expected_count" ]]
  done
  grep -Fq 'JAVA_NESTED_FACET_CARRIER returned nested metadata and count verified' "$log"
  for logging in false true; do
    sink_count=0
    if [[ $logging == true ]]; then sink_count=7; fi
    grep -Fq "JAVA_LOADED_FACET logging=$logging parents=2 independentMembership=true physicalStatements=7 safeSinkStatements=$sink_count" "$log"
    for all in false true; do
      reverse_sink_count=0
      if [[ $logging == true ]]; then reverse_sink_count=10; fi
      grep -Fq "JAVA_GENERATED_REVERSE_FACET logging=$logging includeAll=$all parents=3 fullCounts=true requestedEmpty=true physicalStatements=10 safeSinkStatements=$reverse_sink_count mutationCommands=0" "$log"
      sink_count=0
      if [[ $logging == true ]]; then sink_count=7; fi
      grep -Fq "JAVA_NESTED_FACET {\"logging\":$logging,\"includeAll\":$all,\"visible\":1,\"paymentCount\":2,\"orderCount\":2,\"physicalStatements\":7,\"safeSinkStatements\":$sink_count}" "$log"
      for exists in false true; do
        facet_rows=0
        if [[ $exists == true ]]; then facet_rows=1; fi
        grep -Fq "JAVA_LOADED_EMPTY_FACET logging=$logging includeAll=$all targetExists=$exists facetRows=$facet_rows physicalStatements=4 mutationCommands=1" "$log"
      done
    done
  done
  printf 'PASS Java generated Trace Chain run %s on the same database\n' "$repetition"
done
library_manifest > "$run_dir/library-after.sha256"
cmp "$run_dir/library-before.sha256" "$run_dir/library-after.sha256"
printf 'PASS Java generated library unchanged; evidence and database retained: %s\n' "$run_dir"
