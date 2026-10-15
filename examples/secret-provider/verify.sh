#!/usr/bin/env bash
set -euo pipefail
command -v openssl >/dev/null || { echo 'OpenSSL CLI is required for the local TLS fixture' >&2; exit 1; }
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repo"
for round in 1 2; do
  if ! output="$(mvn -B -q -pl examples/secret-provider -am \
    -Dtest=SecretProviderExampleTest,VaultKv2ExampleTest -Dsurefire.failIfNoSpecifiedTests=false test 2>&1)"; then
    printf 'Java Secret Provider example round %s failed\n%s\n' "$round" "$output" >&2
    exit 1
  fi
  if [[ "$output" == *"private-example-key"* || "$output" == *"PRIVATE-VAULT"* ]]; then
    printf 'Java Secret Provider example round %s leaked a canary\n' "$round" >&2
    exit 1
  fi
  printf 'PASS Java Secret Provider example round %s\n' "$round"
done
