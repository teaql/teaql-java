#!/usr/bin/env bash
set -euo pipefail
mode=off
case "${1:-}" in
  '') ;;
  --default-log) mode=default ;;
  *) printf 'Usage: bash benchmark-cold.sh [--default-log]\n' >&2; exit 2 ;;
esac
(( $# <= 1 )) || exit 2
runtime_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
example_dir="$runtime_dir/examples/shared-load-state"
run_dir="$(mktemp -d -t teaql-java-cold-query.XXXXXXXX)"
printf 'Cold query evidence: %s\n' "$run_dir"
cd "$runtime_dir"
git diff HEAD --exit-code -- examples/shared-load-state teaql-core teaql-runtime teaql-provider-jdbc
git rev-parse HEAD >"$run_dir/base-commit.txt"
java -version >"$run_dir/toolchain.txt" 2>&1
sha256sum examples/shared-load-state/src/test/java/com/example/schoolmanagementservice/GeneratedColdQueryProbe.java >"$run_dir/source.sha256"
rg --files --hidden --no-ignore "$example_dir/target/generated/lib/src" -0 |
  sort -z | xargs -0 sha256sum >"$run_dir/generated-source.sha256"
# Run the local verifier first to provide a reactor-resolved test classpath.
report="$example_dir/target/surefire-reports/TEST-com.example.schoolmanagementservice.GeneratedSchoolLoadStateTest.xml"
[[ -f "$report" ]] || { printf 'Run examples/shared-load-state/verify.sh first\n' >&2; exit 1; }
mvn -q -o -Pshared-load-state-example -pl examples/shared-load-state -am -DskipTests test-compile >"$run_dir/build.log" 2>&1
classpath="$(xmllint --xpath 'string(/testsuite/properties/property[@name="java.class.path"]/@value)' "$report")"
[[ -n "$classpath" ]]
export TEAQL_LOCAL_RUNTIME_ROOT="$runtime_dir"
TEAQL_COLD_LOG_MODE=off java -cp "$classpath" com.example.schoolmanagementservice.GeneratedColdQueryProbe prepare "$run_dir/school.sqlite" >"$run_dir/prepare.log" 2>&1
sha256sum "$run_dir/school.sqlite" >"$run_dir/database.sha256"
for round in 1 2 3 4 5 6 7; do
  TEAQL_COLD_LOG_MODE="$mode" /usr/bin/time -v -o "$run_dir/cpu-$round.txt" \
    java -cp "$classpath" com.example.schoolmanagementservice.GeneratedColdQueryProbe query "$run_dir/school.sqlite" >"$run_dir/round-$round.log" 2>&1
  rg -F 'PASS generated Java process-cold Q/E and shared snapshot' "$run_dir/round-$round.log"
done
sha256sum --check --quiet "$run_dir/database.sha256"
sha256sum --check --quiet "$run_dir/source.sha256"
sha256sum --check --quiet "$run_dir/generated-source.sha256"
