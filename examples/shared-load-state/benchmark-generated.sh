#!/usr/bin/env bash
set -euo pipefail
mode=off
samples=31
for option in "$@"; do
  case "$option" in
    --default-log) mode=default ;;
    --smoke) samples=3 ;;
    *) printf 'Usage: bash benchmark-generated.sh [--default-log] [--smoke]\n' >&2; exit 2 ;;
  esac
done
runtime_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
run_dir="$(mktemp -d -t teaql-java-generated-query.XXXXXXXX)"
printf 'Generated query evidence: %s\n' "$run_dir"
cd "$runtime_dir"
git diff HEAD --exit-code -- examples/shared-load-state teaql-core teaql-runtime teaql-provider-jdbc teaql-sql-portable
git rev-parse HEAD >"$run_dir/base-commit.txt"
java -version >"$run_dir/toolchain.txt" 2>&1
sha256sum examples/shared-load-state/src/test/java/com/example/schoolmanagementservice/GeneratedQueryBenchmarkTest.java >"$run_dir/source.sha256"
rg --files --hidden --no-ignore examples/shared-load-state/target/generated/lib/src -0 |
  sort -z | xargs -0 sha256sum >"$run_dir/generated-source.sha256"
TEAQL_GENERATED_BENCHMARK_LOG_MODE="$mode" /usr/bin/time -v -o "$run_dir/cpu.txt" \
  mvn -q -Pshared-load-state-example -pl examples/shared-load-state -am \
  -Dtest=GeneratedQueryBenchmarkTest -Dsurefire.failIfNoSpecifiedTests=false \
  -Dteaql.generatedBenchmark=true "-Dteaql.generatedSamples=$samples" test >"$run_dir/results.log" 2>&1
rg -F 'PASS generated Java wide dynamic forward reverse Q/E sharing and private divergence benchmark' "$run_dir/results.log"
cp examples/shared-load-state/target/surefire-reports/TEST-com.example.schoolmanagementservice.GeneratedQueryBenchmarkTest.xml "$run_dir/results.xml"
sha256sum --check --quiet "$run_dir/source.sha256"
sha256sum --check --quiet "$run_dir/generated-source.sha256"
