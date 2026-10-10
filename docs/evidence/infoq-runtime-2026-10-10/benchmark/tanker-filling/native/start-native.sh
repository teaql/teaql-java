#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
set -a
source ../matched-six/.benchmark.env
set +a
export PROBE_PORT=18884
v=${BENCHMARK_VARIANT:?variant required}
export PROBE_DB_URL=jdbc:postgresql://127.0.0.1:15432/teaql_benchmark_matched_new
if [ "$v" = legacy ]; then export PROBE_DB_URL=jdbc:postgresql://127.0.0.1:15432/teaql_benchmark_matched_old; fi
if [ "${NATIVE_JVM_CONTROL:-false}" = true ]; then
 [ "$v" = micronaut ]
 exec "$HOME/teaql-startup-benchmark/runtime/amazon-corretto-21.0.12.12.1-linux-x64/bin/java" -Xms128m -Xmx512m -Dlogging.config="$PWD/../matched-six/logback.xml" -Dlogback.configurationFile="$PWD/../matched-six/logback.xml" -cp "$PWD/micronaut/micronaut-probe-1.jar:$PWD/micronaut/lib/*" io.teaql.benchmark.micronaut.Application
fi
if [ "${NATIVE_TRAIN:-false}" = true ]; then
 graal_home=$(find "$PWD/toolchain" -maxdepth 1 -type d -name 'graalvm-jdk-21*' | head -1)
 mkdir -p "$PWD/$v/agent-config"
 agent="-agentlib:native-image-agent=config-output-dir=$PWD/$v/agent-config"
 case "$v" in
  standalone) cp="$PWD/../matched-six/modern/lib/*"; main=com.doublechaintech.liquidfilling.probe.StandaloneProbe ;;
  spring) cp="$PWD/spring/aot:$PWD/spring/lib/*"; main=com.doublechaintech.liquidfilling.probe.ProbeApplication ;;
  legacy) cp="$PWD/legacy/aot:$PWD/legacy/lib/*"; main=com.doublechaintech.liquidfilling.legacyprobe.LegacyProbeApplication ;;
  micronaut) cp="$PWD/micronaut/micronaut-probe-1.jar:$PWD/micronaut/lib/*"; main=io.teaql.benchmark.micronaut.Application ;;
  *) exit 2 ;;
 esac
 exec "$graal_home/bin/java" "$agent" -Dspring.aot.enabled=true -Dlogging.config="$PWD/../matched-six/logback.xml" -Dlogback.configurationFile="$PWD/../matched-six/logback.xml" -cp "$cp" "$main"
fi
exe="$PWD/$v/$v"
if [ "$v" = quarkus ]; then exe="$PWD/$v/quarkus-probe-1-runner"; fi
exec "$exe" -Xms128m -Xmx512m -Dlogging.config="$PWD/../matched-six/logback.xml" -Dlogback.configurationFile="$PWD/../matched-six/logback.xml"
