#!/usr/bin/env bash
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo"
for round in 1 2; do
  echo "Java committed-events example round $round"
  mvn -B -q -pl examples/committed-events -am \
    -Dtest=CommittedEventsExampleTest -Dsurefire.failIfNoSpecifiedTests=false test
done
