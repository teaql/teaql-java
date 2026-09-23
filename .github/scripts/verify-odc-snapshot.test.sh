#!/usr/bin/env bash
set -euo pipefail

test_dir=$(mktemp -d)
trap 'rm -r -- "$test_dir"' EXIT
verify_script="$(dirname "$0")/verify-odc-snapshot.sh"
truncate -s 2M "$test_dir/odc.mv.db"

date -u +%s > "$test_dir/teaql-nvd-refresh.epoch"
bash "$verify_script" "$test_dir"

printf '%s\n' "$(( $(date -u +%s) - 90000 ))" > "$test_dir/teaql-nvd-refresh.epoch"
if bash "$verify_script" "$test_dir"; then
  echo "Stale snapshot was accepted" >&2
  exit 1
fi

printf '%s\n' invalid > "$test_dir/teaql-nvd-refresh.epoch"
if bash "$verify_script" "$test_dir"; then
  echo "Invalid timestamp was accepted" >&2
  exit 1
fi

printf '%s\n' "$(( $(date -u +%s) + 60 ))" > "$test_dir/teaql-nvd-refresh.epoch"
if bash "$verify_script" "$test_dir"; then
  echo "Future timestamp was accepted" >&2
  exit 1
fi

rm -- "$test_dir/teaql-nvd-refresh.epoch"
if bash "$verify_script" "$test_dir"; then
  echo "Missing timestamp was accepted" >&2
  exit 1
fi

date -u +%s > "$test_dir/teaql-nvd-refresh.epoch"
truncate -s 0 "$test_dir/odc.mv.db"
if bash "$verify_script" "$test_dir"; then
  echo "Empty database was accepted" >&2
  exit 1
fi

echo "Snapshot freshness policy tests passed"
