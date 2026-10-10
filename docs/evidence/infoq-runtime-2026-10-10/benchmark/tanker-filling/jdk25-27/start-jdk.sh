#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
set -a
source ./.benchmark.env
set +a
export PROBE_PORT=18884
benchmark_logging="$(pwd)/logback.xml"
jdk=${BENCHMARK_VARIANT##*-jdk}
export BENCHMARK_VARIANT=${BENCHMARK_VARIANT%-jdk*}
benchmark_java=$(find "$PWD/toolchain" -maxdepth 1 -type d -name "jdk-$jdk*" | head -1)/bin/java
case "${BENCHMARK_VARIANT:?variant required}" in
 legacy-spring)
  export PROBE_DB_URL=jdbc:postgresql://127.0.0.1:15432/teaql_benchmark_matched_old
  exec "$benchmark_java" -Xms128m -Xmx512m -Dlogging.config="$benchmark_logging" -Dlogback.configurationFile="$benchmark_logging" -cp './legacy/lib/*' com.doublechaintech.liquidfilling.legacyprobe.LegacyProbeApplication ;;
 modern-quarkus)
  export PROBE_DB_URL=jdbc:postgresql://127.0.0.1:15432/teaql_benchmark_matched_new
  exec "$benchmark_java" -Xms128m -Xmx512m -Dlogging.config="$benchmark_logging" -Dlogback.configurationFile="$benchmark_logging" -jar ./quarkus/quarkus-run.jar ;;
 modern-micronaut)
  export PROBE_DB_URL=jdbc:postgresql://127.0.0.1:15432/teaql_benchmark_matched_new
  exec "$benchmark_java" -Xms128m -Xmx512m -Dlogging.config="$benchmark_logging" -Dlogback.configurationFile="$benchmark_logging" -cp './micronaut/lib/*:./micronaut/micronaut-probe-1.jar' io.teaql.benchmark.micronaut.Application ;;
 modern-spring|modern-classpath|modern-jpms)
  export PROBE_DB_URL=jdbc:postgresql://127.0.0.1:15432/teaql_benchmark_matched_new
  cd modern
  if [ "$BENCHMARK_VARIANT" = modern-jpms ]; then
   exec "$benchmark_java" -Xms128m -Xmx512m -Dlogging.config="$benchmark_logging" -Dlogback.configurationFile="$benchmark_logging" --module-path ./lib --add-modules ALL-MODULE-PATH,jdk.httpserver --module modern.probe/com.doublechaintech.liquidfilling.probe.StandaloneProbe
  fi
  benchmark_main=com.doublechaintech.liquidfilling.probe.StandaloneProbe
  if [ "$BENCHMARK_VARIANT" = modern-spring ]; then benchmark_main=com.doublechaintech.liquidfilling.probe.ProbeApplication; fi
  exec "$benchmark_java" -Xms128m -Xmx512m -Dlogging.config="$benchmark_logging" -Dlogback.configurationFile="$benchmark_logging" -cp './lib/*' "$benchmark_main" ;;
 *) exit 2 ;;
esac
