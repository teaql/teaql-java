#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
run_dir="$(mktemp -d)"
active_pid=""

cleanup() {
  if [[ -n "$active_pid" ]] && kill -0 "$active_pid" 2>/dev/null; then
    kill "$active_pid" 2>/dev/null || true
    wait "$active_pid" 2>/dev/null || true
  fi
  rm -rf "$run_dir"
}
trap cleanup EXIT

mvn -q -f "$repo_dir/pom.xml" \
  -Pruntime-examples \
  -pl examples/conformance,examples/school-management \
  -am package -DskipTests

run_example() {
  local name="$1"
  local jar="$2"
  local marker="$3"
  local log="$run_dir/$name.log"
  local database="$run_dir/$name.db"

  java -jar "$jar" \
    "--spring.datasource.url=jdbc:sqlite:$database" \
    --server.port=0 >"$log" 2>&1 &
  active_pid=$!

  for _ in $(seq 1 240); do
    if grep -Fq "$marker" "$log"; then
      kill "$active_pid" 2>/dev/null || true
      wait "$active_pid" 2>/dev/null || true
      active_pid=""
      printf 'PASS %s\n' "$name"
      return 0
    fi
    if ! kill -0 "$active_pid" 2>/dev/null; then
      wait "$active_pid" || true
      active_pid=""
      printf 'FAIL %s exited before its acceptance marker\n' "$name" >&2
      sed -n '1,240p' "$log" >&2
      return 1
    fi
    sleep 0.25
  done

  printf 'FAIL %s did not emit its acceptance marker within 60 seconds\n' "$name" >&2
  sed -n '1,240p' "$log" >&2
  return 1
}

run_example \
  "conformance" \
  "$repo_dir/examples/conformance/target/deploy/runtime-example-conformance-service-0.0.1-SNAPSHOT.jar" \
  "PASS Java minimum runtime conformance: 8/8"

run_example \
  "school-management" \
  "$repo_dir/examples/school-management/target/deploy/school-management-service-0.0.1-SNAPSHOT.jar" \
  "PASS Java School bootstrap, portable Query, and native SQLite Facet parity"

printf 'PASS Java runtime examples: 2/2\n'
