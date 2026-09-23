#!/usr/bin/env bash
set -euo pipefail

data_dir=${1:?snapshot directory is required}
stamp_file="$data_dir/teaql-nvd-refresh.epoch"
if [[ ! -f "$stamp_file" ]]; then
  echo "Trusted NVD snapshot has no refresh timestamp" >&2
  exit 1
fi
stamp=$(<"$stamp_file")
if [[ ! "$stamp" =~ ^[0-9]{10}$ ]]; then
  echo "Trusted NVD snapshot has an invalid refresh timestamp" >&2
  exit 1
fi
age=$(( $(date -u +%s) - stamp ))
if (( age < 0 || age > 86400 )); then
  echo "Trusted NVD snapshot is stale or has a future timestamp: age=${age}s" >&2
  exit 1
fi
if ! find "$data_dir" -type f -name '*.mv.db' -size +1M -print -quit | grep -q .; then
  echo "Trusted NVD snapshot has no populated H2 database" >&2
  exit 1
fi
echo "Trusted NVD snapshot age: ${age}s"
