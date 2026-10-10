#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
set -a
source ../../matched-six/.benchmark.env
set +a
export PROBE_PORT=18884 PROBE_DB_URL=jdbc:postgresql://127.0.0.1:15432/teaql_benchmark_matched_old
logging="$PWD/../../matched-six/logback.xml"
exec "$PWD/legacy/legacy" -Xms128m -Xmx512m -Dspring.aot.enabled=true -Dlogging.config="$logging" -Dlogback.configurationFile="$logging"
