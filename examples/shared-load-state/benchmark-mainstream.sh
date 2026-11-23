#!/usr/bin/env bash
set -euo pipefail
workspace=false
if [[ "${1:-}" == --workspace ]]; then
  workspace=true
  shift
fi
(( $# == 0 )) || { printf 'Usage: bash benchmark-mainstream.sh [--workspace]\n' >&2; exit 2; }
example_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
runtime_dir="$(cd "$example_dir/../.." && pwd)"
reactor=(mvn -q -Pshared-load-state-example -pl examples/shared-load-state -am)
target="$example_dir/target"
if [[ "$workspace" == true ]]; then
  reactor=(bash "$example_dir/workspace-reactor.sh")
  target="$example_dir/java-app-console/target"
fi
if [[ -n "${TEAQL_MAINSTREAM_EVIDENCE_DIR:-}" ]]; then
  run_dir="$TEAQL_MAINSTREAM_EVIDENCE_DIR"
  [[ "$run_dir" == /* ]]
  mkdir -- "$run_dir"
else
  run_dir="$(mktemp -d -t teaql-java-mainstream.XXXXXXXX)"
fi
printf 'Mainstream benchmark evidence: %s\n' "$run_dir"
cd "$runtime_dir"
git diff HEAD --exit-code -- examples/shared-load-state teaql-core teaql-runtime teaql-sql-portable teaql-provider-jdbc teaql-sqlite
git rev-parse HEAD >"$run_dir/base-commit.txt"
java -version >"$run_dir/toolchain.txt" 2>&1
sha256sum examples/shared-load-state/src/test/java/com/example/schoolmanagementservice/{MainstreamMapperBenchmarkTest,DriverQueryBenchmarkTest,HydrationAllocationTest}.java examples/shared-load-state/pom.xml >"$run_dir/source.sha256"
/usr/bin/time -v -o "$run_dir/cpu.txt" "${reactor[@]}" \
  -Dtest=MainstreamMapperBenchmarkTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Dteaql.ormBenchmark=true -Dteaql.ormSamples=31 test \
  >"$run_dir/results.log" 2>&1
rg -F 'PASS mainstream MyBatis and TeaQL bounded typed results with independent version and prefix filters' "$run_dir/results.log"
cp "$target/surefire-reports/TEST-com.example.schoolmanagementservice.MainstreamMapperBenchmarkTest.xml" "$run_dir/test-result.xml"
sha256sum --check --quiet "$run_dir/source.sha256"
