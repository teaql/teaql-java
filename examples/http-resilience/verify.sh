#!/usr/bin/env bash
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo"
for round in 1 2; do
  echo "Java HTTP resilience example round $round"
  mvn -B -q -pl examples/http-resilience -am \
    -Dtest=HttpResilienceExampleTest -Dsurefire.failIfNoSpecifiedTests=false test
done
