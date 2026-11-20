#!/usr/bin/env bash
set -euo pipefail
example_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
runtime_dir="$(cd "$example_dir/../.." && pwd)"
generate=false
workspace=false
wide=false
inheritance=false
for argument in "$@"; do
  case "$argument" in
    --workspace) workspace=true ;;
    --generate) generate=true ;;
    --wide) wide=true ;;
    --inheritance) inheritance=true ;;
    *) printf 'Usage: bash verify.sh --workspace | [--generate] [--wide] [--inheritance]\n' >&2; exit 2 ;;
  esac
done
fixture_dir="$example_dir/target/generated"
if [[ "$workspace" == true ]]; then
  if [[ "$generate" == true || "$wide" == true || "$inheritance" == true ]]; then
    printf 'Choose the narrow workspace or a separate generated fixture.\n' >&2
    exit 2
  fi
  fixture_dir="$example_dir/java-lib-core"
fi
if [[ "$generate" == true ]]; then
  : "${TEAQL_CODEGEN_DIR:?Set TEAQL_CODEGEN_DIR to the local generator checkout}"
  mvn -q -f "$TEAQL_CODEGEN_DIR/pom.xml" -pl generator -am \
    '-Dtest=JavaGeneratedMysqlSqliteIntegrationTest#generatedJavaSharedLoadStateRuntimeExample' \
    -Dexec.skip=true -Dsurefire.failIfNoSpecifiedTests=false -Dteaql.loadState.generateOnly=true "-Dteaql.loadState.wide=$wide" "-Dteaql.loadState.inheritance=$inheritance" \
    "-Dteaql.java.dir=$runtime_dir" test
fi
fixture_mode=narrow
if [[ -f "$fixture_dir/fixture-mode.txt" ]]; then
  IFS= read -r fixture_mode <"$fixture_dir/fixture-mode.txt"
fi
if [[ "$wide" == true && "$fixture_mode" != wide ]]; then
  printf 'Wide verification requires a newly generated wide fixture.\n' >&2
  exit 1
fi
case "$fixture_mode" in
  wide) export TEAQL_LOAD_STATE_WIDE=true ;;
  narrow) export TEAQL_LOAD_STATE_WIDE=false ;;
  *) printf 'Invalid generated fixture mode\n' >&2; exit 1 ;;
esac
fixture_inheritance=false
if [[ -f "$fixture_dir/fixture-inheritance.txt" ]]; then
  IFS= read -r fixture_inheritance <"$fixture_dir/fixture-inheritance.txt"
fi
if [[ "$inheritance" == true && "$fixture_inheritance" != true ]]; then
  printf 'Inheritance verification requires a newly generated inherited fixture.\n' >&2
  exit 1
fi
test_targets=GeneratedSchoolLoadStateTest,TypedStreamAllocationTest
case "$fixture_inheritance" in
  true) test_targets+=,GeneratedInheritedSchoolLoadStateTest ;;
  false) ;;
  *) printf 'Invalid generated inheritance mode\n' >&2; exit 1 ;;
esac
if [[ ! -f "$fixture_dir/lib/pom.xml" ]]; then
  printf 'Generate first with TEAQL_CODEGEN_DIR=<local generator> bash verify.sh --generate\n' >&2
  exit 1
fi
if [[ -n "${TEAQL_LOAD_STATE_EVIDENCE_DIR:-}" ]]; then
  run_dir="$TEAQL_LOAD_STATE_EVIDENCE_DIR"
  [[ "$run_dir" == /* ]]
  mkdir -- "$run_dir"
else
  run_dir="$(mktemp -d -t teaql-generated-state.XXXXXXXX)"
  if [[ "$workspace" == true ]]; then
    trap 'rm -rf -- "$run_dir"' EXIT
  fi
fi
printf 'School load-state evidence: %s\n' "$run_dir"
export TEAQL_LOCAL_RUNTIME_ROOT="$runtime_dir"
export TEAQL_LOAD_STATE_DATABASE="$run_dir/school.sqlite"
rg --files --hidden --no-ignore "$fixture_dir/lib/src" -0 |
  sort -z | xargs -0 sha256sum >"$run_dir/generated-source.sha256"
reactor=(mvn -q -f "$runtime_dir/pom.xml" -Pshared-load-state-example -pl examples/shared-load-state -am)
if [[ "$workspace" == true ]]; then
  reactor=(bash "$example_dir/workspace-reactor.sh")
fi
for round in first second; do
  export TEAQL_LOAD_STATE_ROUND="$round"
  "${reactor[@]}" \
    "-Dtest=$test_targets" "-Dteaql.loadState.inheritance=$fixture_inheritance" -Dsurefire.failIfNoSpecifiedTests=false test \
    >"$run_dir/$round.log" 2>&1 || {
      tail -80 "$run_dir/$round.log" >&2
      exit 1
    }
  if [[ "$workspace" == true ]]; then
    rg -Fx -- "LOCAL_CLASS com.example.schoolmanagementservice.school.School $fixture_dir/lib/target/classes" "$run_dir/$round.log"
    cp "$example_dir/java-app-console/target/surefire-reports/"TEST-*.xml "$run_dir/"
  fi
  rg -F "PASS generated Java indexed Q/E/Checker/create/update/delete and snapshot sharing $round" "$run_dir/$round.log"
  rg -F "PASS generated Java typed native JSON roundtrip and snapshot sharing $round" "$run_dir/$round.log"
  rg -F "PASS generated Java reserved runtime JSON keys reject without provider entry or value leakage" "$run_dir/$round.log"
  rg -F "PASS generated Java typed JSON graph roundtrip and Empty/NotLoaded isolation" "$run_dir/$round.log"
  rg -F "PASS generated Java sparse Checker rejects before provider entry" "$run_dir/$round.log"
  rg -F "PASS generated Java complete parent saves with untouched partial reference" "$run_dir/$round.log"
  rg -F "PASS generated Java modified partial referenced object rejects before provider entry without loading missing business fields" "$run_dir/$round.log"
  rg -F "PASS generated Java empty native value stays loaded without widening absent fields" "$run_dir/$round.log"
  rg -F "PASS generated Java Q selection and real JDBC column-order invariance" "$run_dir/$round.log"
  rg -F "PASS generated Java page and stream shared load state" "$run_dir/$round.log"
  rg -F "PASS generated Java readonly property metadata shares schema without installing values or fixed slots" "$run_dir/$round.log"
  rg -F "PASS generated Java dynamic stream Value/Null/NotLoaded and shared snapshots" "$run_dir/$round.log"
  rg -F "PASS generated Java dynamic stream full/overflow/sparse state and Checker-before-provider" "$run_dir/$round.log"
  rg -F "PASS generated Java Checker preserves NotLoaded and sibling load boundaries" "$run_dir/$round.log"
  rg -F "PASS generated Java dynamic storage provenance and retry" "$run_dir/$round.log"
  rg -F "PASS generated Java mixed dynamic Value/Null/NotLoaded list lifetime readback rollback and retry" "$run_dir/$round.log"
  rg -F "PASS generated Java stored unselected extension survives rollback retry and readonly property is not persisted" "$run_dir/$round.log"
  rg -F "PASS generated Java namespace serialization and NotLoaded boundary" "$run_dir/$round.log"
  rg -F "PASS generated Java nested/reverse graph Q/E/JSON and Empty/NotLoaded isolation" "$run_dir/$round.log"
  rg -F "PASS generated Java nested dynamic Value/NULL/NotLoaded and shared snapshots" "$run_dir/$round.log"
  rg -F "PASS generated Java LF08 loaded FK and excluded forward details stay distinct" "$run_dir/$round.log"
  rg -F "PASS generated Java LF08 exact NotLoaded type and path in Q and typed JSON" "$run_dir/$round.log"
  rg -F "PASS generated Java LF09 reverse Loaded/Empty/NotLoaded through Q/E/JSON" "$run_dir/$round.log"
  rg -F "PASS generated Java LF17 dynamic metadata shared without fixed slots" "$run_dir/$round.log"
  rg -F "PASS generated Java LF19 fixed derived and persistent same-name namespace isolation" "$run_dir/$round.log"
  rg -F "PASS generated Java LF23 dynamic availability detaches only one view" "$run_dir/$round.log"
  rg -F "PASS generated Java value-only dynamic mutation retains shared snapshot and sibling payload" "$run_dir/$round.log"
  rg -F "PASS generated Java saving one dirty root leaves another dirty root unpersisted" "$run_dir/$round.log"
  rg -F "PASS generated Java LF20 readonly total persists only through modeled materialization" "$run_dir/$round.log"
  rg -F "PASS generated Java LF11 native readback rollback retains loaded state and retry intent" "$run_dir/$round.log"
  rg -F "PASS generated Java authoritative missing column rolls back without turning absence into null" "$run_dir/$round.log"
  rg -F "PASS generated Java homogeneous list finalization allocates zero and preserves shared state" "$run_dir/$round.log"
  rg -F "PASS Java real typed stream loaded state and provider allocation controls" "$run_dir/$round.log"
  if [[ "$fixture_inheritance" == true ]]; then
    rg -F "PASS generated Java inherited indexes Q/E/save and snapshot isolation $round" "$run_dir/$round.log"
  fi
done
sha256sum --check --quiet "$run_dir/generated-source.sha256"
printf 'PASS fresh generated Java shared-load-state twice\n'
