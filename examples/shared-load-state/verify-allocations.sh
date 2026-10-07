#!/usr/bin/env bash
set -euo pipefail
example_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
runtime_dir="$(cd "$example_dir/../.." && pwd)"
test_targets=LoadStateAllocationTest,HydrationAllocationTest
wide=false
case "${1:-}" in
  "") ;;
  --wide) wide=true; test_targets+=,GeneratedWideHydrationAllocationTest,GeneratedReadbackAllocationTest ;;
  *) printf 'Usage: bash verify-allocations.sh [--wide]\n' >&2; exit 2 ;;
esac
if (( $# > 1 )); then printf 'Unexpected arguments\n' >&2; exit 2; fi
if [[ ! -f "$example_dir/target/generated/lib/pom.xml" ]]; then
  printf 'Run the generated example verification with --generate first.\n' >&2
  exit 1
fi
if [[ "$wide" == true ]]; then
  mode=""
  IFS= read -r mode <"$example_dir/target/generated/fixture-mode.txt"
  [[ "$mode" == wide ]] || { printf 'Generate a wide fixture before wide allocation verification\n' >&2; exit 1; }
fi
run_dir="$(mktemp -d -t teaql-java-state-allocations.XXXXXXXX)"
printf 'Evidence retained at %s\n' "$run_dir"
java -version >"$run_dir/toolchain.txt" 2>&1
git -C "$runtime_dir" rev-parse HEAD >"$run_dir/base-commit.txt"
git -C "$runtime_dir" diff --exit-code -- examples/shared-load-state teaql-core teaql-runtime teaql-sql-portable teaql-provider-jdbc teaql-sqlite
(
  cd "$runtime_dir"
  git ls-files -z teaql-core/src teaql-runtime/src teaql-sql-portable/src teaql-provider-jdbc/src teaql-sqlite/src examples/shared-load-state/src examples/shared-load-state/verify-allocations.sh |
    xargs -0 sha256sum
) >"$run_dir/source.sha256"
rg --files --hidden --no-ignore "$example_dir/target/generated/lib/src" -0 |
  sort -z | xargs -0 sha256sum >"$run_dir/generated-source.sha256"
mvn -q -f "$runtime_dir/pom.xml" -Pshared-load-state-example \
  -pl examples/shared-load-state -am "-Dtest=$test_targets" \
  -Dsurefire.failIfNoSpecifiedTests=false test >"$run_dir/probe.log" 2>&1 || {
    tail -80 "$run_dir/probe.log" >&2
    exit 1
  }
printf 'case,width,iterations,allocated_bytes,elapsed_ns\n' >"$run_dir/allocations.csv"
rg '^(loaded_|reference_)' "$run_dir/probe.log" >>"$run_dir/allocations.csv"
printf 'case,projection,rows,allocated_bytes,elapsed_ns\n' >"$run_dir/hydration.csv"
rg '^(compiled_hydration|map_hydration|plain_hydration),' "$run_dir/probe.log" >>"$run_dir/hydration.csv"
printf 'case,lane,projection,rows,allocated_bytes\n' >"$run_dir/stream-hydration.csv"
rg '^STREAM_HYDRATION,' "$run_dir/probe.log" >>"$run_dir/stream-hydration.csv"
printf 'case,wide,rows,allocated_bytes\n' >"$run_dir/list-finalization.csv"
rg '^LIST_FINALIZE,' "$run_dir/probe.log" >>"$run_dir/list-finalization.csv"
rg -F 'PASS mixed map/parallel-stream projection and dynamic-property NULL presence' "$run_dir/probe.log"
if [[ "$wide" == true ]]; then
  printf 'case,entity,selected_fields,rows,allocated_bytes,elapsed_ns\n' >"$run_dir/wide-hydration.csv"
  rg '^generated_wide_hydration,' "$run_dir/probe.log" >>"$run_dir/wide-hydration.csv"
  rg -F 'PASS generated Java wide hydration shares one overflow snapshot per shape' "$run_dir/probe.log"
  printf 'case,rows,incremental_reference_bytes,current_bytes\n' >"$run_dir/native-readback.csv"
  rg '^NATIVE_READBACK_ALLOC,' "$run_dir/probe.log" >>"$run_dir/native-readback.csv"
  rg -F 'PASS generated Java authoritative readback reuses one actual shape and avoids per-field overflow copies' "$run_dir/probe.log"
fi
sha256sum --check --quiet "$run_dir/generated-source.sha256"
(cd "$runtime_dir" && sha256sum --check --quiet "$run_dir/source.sha256")
printf 'PASS Java warmed load-state allocation probe\n'
