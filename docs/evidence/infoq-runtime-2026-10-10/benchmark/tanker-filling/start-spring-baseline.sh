#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
benchmark_java="$HOME/teaql-startup-benchmark/runtime/amazon-corretto-17.0.20.12.1-linux-x64/bin/java"
if [ -f .benchmark.env ]; then
  set -a
  source ./.benchmark.env
  set +a
fi
: "${BENCHMARK_DB_PASSWORD:?Set the isolated PostgreSQL password before starting}"
exec "$benchmark_java" -Xms128m -Xmx512m -jar ./app.jar \
  --spring.config.location=file:./spring-baseline.properties \
  --spring.profiles.active=benchmark \
  --teaql.ensureTable="${BENCHMARK_ENSURE_TABLE:-false}"
