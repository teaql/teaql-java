#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
set -a
source ./.benchmark.env
set +a
export PROBE_DB_URL=jdbc:postgresql://127.0.0.1:15432/teaql_benchmark_modern_clean
case "${PROBE_MODE:-spring}" in
  spring)
    export PROBE_PORT="${PROBE_PORT:-18881}"
    benchmark_main=com.doublechaintech.liquidfilling.probe.ProbeApplication
    ;;
  standalone)
    export PROBE_PORT="${PROBE_PORT:-18882}"
    benchmark_main=com.doublechaintech.liquidfilling.probe.StandaloneProbe
    ;;
  jpms)
    export PROBE_PORT="${PROBE_PORT:-18882}"
    benchmark_main=com.doublechaintech.liquidfilling.probe.StandaloneProbe
    ;;
  *) echo 'Unknown probe mode' >&2; exit 1 ;;
esac
benchmark_java="$HOME/teaql-startup-benchmark/runtime/amazon-corretto-21.0.12.12.1-linux-x64/bin/java"
if [ "${PROBE_MODE:-spring}" = jpms ]; then
  exec "$benchmark_java" -Xms128m -Xmx512m \
    --module-path ./lib --add-modules ALL-MODULE-PATH,jdk.httpserver \
    --module modern.probe/"$benchmark_main"
fi
exec "$benchmark_java" \
  -Xms128m -Xmx512m -cp './lib/*' "$benchmark_main"
