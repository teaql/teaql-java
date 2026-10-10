#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
# The legacy configured build is last in the explicitly serial build queue.
while ! grep -q 'Exit status: 0' legacy/build-configured.log 2>/dev/null || ! grep -q 'Exit status: 0' micronaut/build-configured.log 2>/dev/null; do sleep 5; done
for v in standalone spring quarkus micronaut legacy; do
 grep -q 'Exit status: 0' "$v/build-configured.log" || { echo "FAILED BUILD $v"; exit 1; }
done
python3 verify_contract.py
cp contract-verification.json contract-pre-campaign.json
python3 capture_provenance.py
python3 collect_native.py --runs 30 --load-repeats 5
NATIVE_VARIANTS=micronaut NATIVE_JVM_CONTROL=true python3 verify_contract.py
NATIVE_VARIANTS=micronaut python3 collect_micronaut_control.py --runs 30 --load-repeats 5
python3 verify_contract.py
cp contract-verification.json contract-post-campaign.json
