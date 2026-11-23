#!/usr/bin/env bash
set -euo pipefail
command -v openssl >/dev/null || { echo 'OpenSSL CLI is required for the local TLS fixture' >&2; exit 1; }
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo"
evidence_dir="${TEAQL_SECRET_EVIDENCE_DIR:-}"
if [[ -n "$evidence_dir" ]]; then
  [[ "$evidence_dir" == /* && ! -e "$evidence_dir" && ! -L "$evidence_dir" ]] || {
    echo 'Use a new absolute Secret Provider evidence directory' >&2; exit 1;
  }
  mkdir -p "$evidence_dir"
fi
for round in 1 2; do
  native_status=0
  output="$(mvn -B -q -pl examples/secret-provider -am \
    -Dtest=SecretProviderExampleTest,VaultKv2ExampleTest -Dsurefire.failIfNoSpecifiedTests=false test 2>&1)" || native_status=$?
  if [[ -n "$evidence_dir" ]]; then
    printf '%s\n' "$output" >"$evidence_dir/native-$round.log"
  fi
  if (( native_status != 0 )); then
    printf 'Java Secret Provider example round %s failed\n%s\n' "$round" "$output" >&2
    exit "$native_status"
  fi
  if [[ "$output" == *"private-example-key"* || "$output" == *"PRIVATE-VAULT"* ]]; then
    printf 'Java Secret Provider example round %s leaked a canary\n' "$round" >&2
    exit 1
  fi
  if [[ "$output" != *"SECRET_ROTATION_LEDGER_UNCHANGED_PASS language=java context=1 clock=1 pending=1"* ||
        "$output" != *"SECRET_CONTEXT_LEDGER_ISOLATION_PASS language=java overlap=1 same_id=1 clear_isolated=1"* ]]; then
    printf 'Java Secret Provider example round %s lacked rotation/isolation evidence\n' "$round" >&2
    exit 1
  fi
  printf 'PASS Java Secret Provider example round %s\n' "$round"
done
