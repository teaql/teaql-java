#!/usr/bin/env bash
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo"
evidence="${TEAQL_HTTP_EXAMPLE_EVIDENCE_DIR:-}"
if [[ -n "$evidence" ]]; then
  [[ ! -e "$evidence" ]] || { echo 'HTTP example evidence must be fresh' >&2; exit 1; }
  mkdir -p "$evidence"
fi
for round in 1 2; do
  echo "Java HTTP resilience example round $round"
  mvn -B -q -pl examples/http-resilience -am \
    -Dtest=HttpResilienceExampleTest -Dsurefire.failIfNoSpecifiedTests=false test
  if [[ -n "$evidence" ]]; then
    cp examples/http-resilience/target/surefire-reports/TEST-io.teaql.examples.http.HttpResilienceExampleTest.xml \
      "$evidence/round-$round.xml"
  fi
done
