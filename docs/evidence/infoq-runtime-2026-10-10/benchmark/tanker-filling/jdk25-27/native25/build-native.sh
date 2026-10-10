#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
graal_home=$(find -H "$PWD/toolchain" -maxdepth 1 -type d -name 'graalvm-jdk-25*' | head -1)
export JAVA_HOME="$graal_home" PATH="$graal_home/bin:$PATH"
variant=${1:?standalone|spring|legacy|quarkus|micronaut}
cd "$variant"
log=build-initial.log
if [ "${NATIVE_METADATA:-false}" = true ]; then log=build-configured.log; fi
common=(-J-Xmx8g --parallelism=4 --no-fallback -march=compatibility -O2 --gc=serial)
if [ "${NATIVE_METADATA:-false}" = true ]; then config_dirs=../common-config
 if [ -d agent-config ]; then config_dirs="$config_dirs,agent-config"; fi
 common+=(-H:ConfigurationFileDirectories="$config_dirs"); fi
case "$variant" in
 standalone) args=(--add-modules=jdk.httpserver -cp '../../../matched-six/modern/lib/*' com.doublechaintech.liquidfilling.probe.StandaloneProbe standalone) ;;
 spring) args=(-cp 'aot:lib/*' com.doublechaintech.liquidfilling.probe.ProbeApplication spring) ;;
 legacy)
  args=(-cp 'aot:lib/*' com.doublechaintech.liquidfilling.legacyprobe.LegacyProbeApplication legacy)
  common+=('--initialize-at-build-time=org.redisson.misc.BiHashMap,org.redisson.liveobject.core.RedissonObjectBuilder$CodecMethodRef')
  ;;
 micronaut)
  args=(-cp 'micronaut-probe-1.jar:lib/*' io.teaql.benchmark.micronaut.Application micronaut)
  if [ "${NATIVE_METADATA:-false}" = true ]; then
   common+=(-H:+SharedArenaSupport --initialize-at-run-time=io.netty,org.slf4j,ch.qos.logback
    --exclude-config 'netty-codec-http-.*' '/META-INF/native-image/io.netty/netty-codec-http/native-image\.properties'
    --exclude-config 'netty-codec-http2-.*' '/META-INF/native-image/io.netty/netty-codec-http2/native-image\.properties')
  fi
  ;;
 quarkus)
  if [ "${NATIVE_METADATA:-false}" = true ]; then
   sed "s/--link-at-build-time /--link-at-build-time=io.quarkus,io.teaql /" native-image.args > native-image-configured.args
   args=(@native-image-configured.args -H:+UseServiceLoaderFeature)
  else args=(@native-image.args); fi ;;
 *) exit 2 ;;
esac
/usr/bin/time -v native-image "${common[@]}" "${args[@]}" > "$log" 2>&1
