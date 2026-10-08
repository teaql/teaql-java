#!/usr/bin/env bash
set -euo pipefail
(( $# == 0 )) || { printf 'Usage: bash benchmark-mainstream.sh\n' >&2; exit 2; }
runtime_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
run_dir="$(mktemp -d -t teaql-java-mainstream.XXXXXXXX)"
printf 'Mainstream benchmark evidence: %s\n' "$run_dir"
cd "$runtime_dir"
git diff HEAD --exit-code -- examples/shared-load-state teaql-core teaql-runtime teaql-sql-portable teaql-provider-jdbc teaql-sqlite
git rev-parse HEAD >"$run_dir/base-commit.txt"
java -version >"$run_dir/toolchain.txt" 2>&1
sha256sum examples/shared-load-state/src/test/java/com/example/schoolmanagementservice/{MainstreamMapperBenchmarkTest,DriverQueryBenchmarkTest,HydrationAllocationTest}.java examples/shared-load-state/pom.xml >"$run_dir/source.sha256"
/usr/bin/time -v -o "$run_dir/cpu.txt" mvn -q -Pshared-load-state-example \
  -pl examples/shared-load-state -am -Dtest=MainstreamMapperBenchmarkTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Dteaql.ormBenchmark=true -Dteaql.ormSamples=31 test \
  >"$run_dir/results.log" 2>&1
rg -F 'PASS mainstream MyBatis and TeaQL bounded typed results with independent version and prefix filters' "$run_dir/results.log"
cp examples/shared-load-state/target/surefire-reports/TEST-com.example.schoolmanagementservice.MainstreamMapperBenchmarkTest.xml "$run_dir/test-result.xml"
sha256sum --check --quiet "$run_dir/source.sha256"
