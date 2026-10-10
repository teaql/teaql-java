#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
set -a
source ./.benchmark.env
set +a
export PROBE_PORT=18884 PROBE_DB_URL=jdbc:postgresql://127.0.0.1:15432/teaql_benchmark_matched_new
jdk=${BENCHMARK_VARIANT##*-jdk}
v=${BENCHMARK_VARIANT%-jdk*}
java=$(find "$PWD/toolchain" -maxdepth 1 -type d -name "jdk-$jdk*" | head -1)/bin/java
logging="$PWD/logback.xml"
case "$v" in
 current-spring) exec "$java" -Xms128m -Xmx512m -Dlogging.config="$logging" -Dlogback.configurationFile="$logging" -cp './current-spring/lib/*:./current-spring/spring-probe25-1.jar' com.doublechaintech.liquidfilling.probe.ProbeApplication ;;
 current-micronaut) exec "$java" -Xms128m -Xmx512m -Dlogging.config="$logging" -Dlogback.configurationFile="$logging" -cp './current-micronaut/lib/*:./current-micronaut/micronaut-probe-1.jar' io.teaql.benchmark.micronaut.Application ;;
 current-quarkus) exec "$java" -Xms128m -Xmx512m -jar ./current-quarkus/quarkus-run.jar ;;
 *) exit 2 ;;
esac
