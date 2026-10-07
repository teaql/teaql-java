#!/usr/bin/env bash
set -euo pipefail
runtime_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
run_dir="$(mktemp -d -t teaql-java-driver-evidence.XXXXXXXX)"
cd "$runtime_dir"
java -version >"$run_dir/toolchain.txt" 2>&1
git rev-parse HEAD >"$run_dir/base-commit.txt"
sha256sum examples/shared-load-state/src/test/java/com/example/schoolmanagementservice/{DriverQueryBenchmarkTest,HydrationAllocationTest}.java >"$run_dir/source.sha256"
# Requires the example's locally generated fixture, as for verify.sh. This
# CPU receipt includes Maven compilation/JIT and is not query CPU attribution.
/usr/bin/time -v -o "$run_dir/maven-cpu.txt" mvn -q -Pshared-load-state-example \
  -pl examples/shared-load-state -am -Dtest=DriverQueryBenchmarkTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Dteaql.driverBenchmark=true test \
  >"$run_dir/results.log" 2>&1
rg -F 'PASS matched JDBC typed results and shared snapshots' "$run_dir/results.log"
sha256sum --check --quiet "$run_dir/source.sha256"
printf 'Driver benchmark evidence: %s\n' "$run_dir"
