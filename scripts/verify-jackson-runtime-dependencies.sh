#!/usr/bin/env bash
set -euo pipefail

repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo"

# Generated applications have their own dependency management. This gate
# verifies published runtime modules and the parent-owned Business ID example.
tree=$(mvn -B -pl '!examples/conformance/lib,!examples/conformance' \
  dependency:tree \
  -Dincludes=com.fasterxml.jackson.core:jackson-databind,com.fasterxml.jackson.core:jackson-core,com.fasterxml.jackson.core:jackson-annotations \
  -DoutputType=text)

for expected in \
  'jackson-databind:jar:2.22.3:' \
  'jackson-core:jar:2.22.3:' \
  'jackson-annotations:jar:2.22:'; do
  if [[ "$tree" != *"$expected"* ]]; then
    echo "Expected Jackson dependency not found: $expected" >&2
    exit 1
  fi
done

unexpected=$(printf '%s\n' "$tree" | awk '
  /com\.fasterxml\.jackson\.core:jackson-databind:jar:/ && !/jackson-databind:jar:2\.22\.3:/ { print }
  /com\.fasterxml\.jackson\.core:jackson-core:jar:/ && !/jackson-core:jar:2\.22\.3:/ { print }
  /com\.fasterxml\.jackson\.core:jackson-annotations:jar:/ && !/jackson-annotations:jar:2\.22:/ { print }
')
if [[ -n "$unexpected" ]]; then
  echo "Runtime reactor resolved a conflicting Jackson version:" >&2
  printf '%s\n' "$unexpected" >&2
  exit 1
fi

echo "PASS: runtime Jackson dependencies follow the managed BOM"
