#!/usr/bin/env bash
set -euo pipefail
example="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$example/../.." && pwd)"
if [[ -n "${TEAQL_FREIGHT_QUOTE_EVIDENCE_DIR:-}" ]]; then
  evidence="$TEAQL_FREIGHT_QUOTE_EVIDENCE_DIR"
  mkdir "$evidence"
else
  evidence="$(mktemp -d -t teaql-java-freight-evidence.XXXXXXXX)"
fi
evidence="$(cd "$evidence" && pwd)"
scratch="$(mktemp -d -t teaql-java-freight-build.XXXXXXXX)"
cleanup() {
  status=$?
  trap - EXIT
  rm -rf -- "$scratch" || { [[ "$status" != 0 ]] || status=1; }
  exit "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

source="$scratch/source"
mkdir "$source"
git -C "$repo" archive HEAD | tar -xf - -C "$source"
app="$source/examples/scalar-type-probe/java-app-console"
library="$source/examples/scalar-type-probe/java-lib-core/lib"
cp "$app/pom.xml" "$evidence/workspace-pom.xml"
diff -qr "$app/lib/src" "$library/src"
cat > "$scratch/pom.xml" <<'XML'
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>io.teaql.examples</groupId>
  <artifactId>freight-quote-source-reactor</artifactId>
  <version>1.0.0</version>
  <packaging>pom</packaging>
  <modules>
    <module>source</module>
    <module>source/examples/scalar-type-probe/java-lib-core/lib</module>
    <module>source/examples/scalar-type-probe/java-app-console</module>
  </modules>
</project>
XML
mvn -o -B -ntp -f "$scratch/pom.xml" -pl :freight-quote-settlement -am package -DskipTests > "$evidence/build.log" 2>&1 || {
  tail -80 "$evidence/build.log"
  exit 1
}
artifact="$app/target/freight-quote-settlement-0.0.1-SNAPSHOT.jar"
sha256sum "$artifact" > "$evidence/application.sha256"
class_origins=(
  'teaql-core:io/teaql/core/UserContext.class'
  'teaql-runtime:io/teaql/runtime/TeaQLRuntime.class'
  'teaql-sqlite:io/teaql/core/sqlite/SqliteDecimalFunctions.class'
  'teaql-jackson:io/teaql/jackson/TeaQLModule.class'
)
for origin in "${class_origins[@]}"; do
  module="${origin%%:*}"
  entry="${origin#*:}"
  unzip -p "$artifact" "$entry" | cmp - "$source/$module/target/classes/$entry"
  sha256sum "$source/$module/target/classes/$entry"
done > "$evidence/runtime-classes.sha256"
for phase in first restart; do
  java -cp "$artifact" com.example.freightquotesettlement.FreightQuoteShowcase "$evidence/freight.sqlite" "$phase" | tee "$evidence/$phase.log"
done
mkdir "$evidence/calibration"
for name in scalar finite restoration-views outer-replay jdbc-exact sql-millis empty memory-empty; do
  options=()
  if [[ "$name" != scalar ]]; then options+=("--$name"); fi
  TEAQL_JAVA_SCALAR_EVIDENCE_DIR="$evidence/calibration/$name" node "$example/verify-local.cjs" "${options[@]}" | tee "$evidence/calibration/$name.log"
done
for name in profiles lifecycle numeric temporal local-datetime rejected explicit-null loaded-revert version-lifecycle; do
  options=()
  if [[ "$name" != profiles ]]; then options+=("--$name"); fi
  TEAQL_JAVA_JSON_PROFILE_EVIDENCE_DIR="$evidence/calibration/json-$name" node "$example/verify-json-profiles.cjs" "${options[@]}" | tee "$evidence/calibration/json-$name.log"
done
echo "PASS Freight quotes and scalar calibrations. Evidence at $evidence"
