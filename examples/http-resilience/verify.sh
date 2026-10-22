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
  result=0
  mvn -B -q -pl examples/http-resilience -am \
    -Dtest=HttpResilienceExampleTest -Dsurefire.failIfNoSpecifiedTests=false test || result=$?
  if [[ -n "$evidence" && -f examples/http-resilience/target/surefire-reports/TEST-io.teaql.examples.http.HttpResilienceExampleTest.xml ]]; then
    if ! cp examples/http-resilience/target/surefire-reports/TEST-io.teaql.examples.http.HttpResilienceExampleTest.xml \
      "$evidence/round-$round.xml"; then
      (( result != 0 )) || result=1
    fi
  fi
  (( result == 0 )) || exit "$result"
done
mvn -B -q -pl teaql-tool-http -am \
  -Dtest=HttpProfileTrustTest -Dsurefire.failIfNoSpecifiedTests=false test
echo 'HTTP_TRUST_EXAMPLE_PASS language=java tests=5'
