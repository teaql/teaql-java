#!/usr/bin/env bash
set -euo pipefail

fixture_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
runtime_root="$(cd "$fixture_dir/../.." && pwd)"
generator_root="${TEAQL_CODEGEN_DIR:?Set TEAQL_CODEGEN_DIR to the local teaql-code-gen checkout}"
generator_root="$(cd "$generator_root" && pwd)"
run_dir="$(mktemp -d -t teaql-java-exact-decimal.XXXXXX)"
model_file="$fixture_dir/model.xml"

command -v mvn >/dev/null
command -v rg >/dev/null
mvn -q -f "$generator_root/pom.xml" -pl model -am -DskipTests install \
  >"$run_dir/model-install.log" 2>&1
mvn -q -f "$generator_root/pom.xml" -pl generator -am -DskipTests compile \
  >"$run_dir/generator-compile.log" 2>&1
mvn -q -f "$generator_root/pom.xml" -pl generator exec:java \
  -Dexec.mainClass=com.skynet.codegenerator.LocalGenerator \
  -Dexec.args="java-app-console $run_dir/java-app-console $model_file" \
  >"$run_dir/generation.log" 2>&1
mvn -q -f "$runtime_root/pom.xml" -pl teaql-sqlite -am \
  -DskipTests install >"$run_dir/runtime-install.log" 2>&1

app_dir="$run_dir/java-app-console"
app_src="$app_dir/src/main/java/com/example/exactdecimalservice"
cp "$fixture_dir/GeneratedExactDecimalProbe.java" "$app_src/GeneratedExactDecimalProbe.java"
cp "$fixture_dir/GeneratedWorkspaceStartupProbe.java" "$app_src/GeneratedWorkspaceStartupProbe.java"
library_hash_before="$(cd "$app_dir/lib" && rg --files src | LC_ALL=C sort \
  | xargs sha256sum | sha256sum | cut -d' ' -f1)"
mvn -q -f "$app_dir/lib/pom.xml" -DskipTests install \
  >"$run_dir/library-install.log" 2>&1

for round in 1 2; do
  if ! mvn -q -f "$app_dir/pom.xml" -DskipTests compile exec:java \
      -Dexec.mainClass=com.example.exactdecimalservice.GeneratedExactDecimalProbe \
      >"$run_dir/round-$round.log" 2>&1; then
    echo "Generated exact Decimal probe failed: $run_dir/round-$round.log" >&2
    exit 1
  fi
  if ! rg -q '^GENERATED_EXACT_DECIMAL_PASS q=1 e=1 save=5 update=1 sum=0.3 scale=3.50 precision=19 exponent=2$' \
      "$run_dir/round-$round.log"; then
    echo "Generated exact Decimal probe lacks its complete marker: $run_dir/round-$round.log" >&2
    exit 1
  fi
  if ! timeout 35s mvn -q -f "$app_dir/pom.xml" -DskipTests compile exec:java \
      -Dexec.mainClass=com.example.exactdecimalservice.GeneratedWorkspaceStartupProbe \
      -Dexec.args="--spring.datasource.url=jdbc:sqlite:$run_dir/workspace.sqlite" \
      >"$run_dir/workspace-$round.log" 2>&1; then
    echo "Generated SQLite workspace failed: $run_dir/workspace-$round.log" >&2
    exit 1
  fi
  if ! rg -q '^GENERATED_EXACT_DECIMAL_WORKSPACE_PASS$' "$run_dir/workspace-$round.log"; then
    echo "Generated SQLite workspace lacks its complete marker: $run_dir/workspace-$round.log" >&2
    exit 1
  fi
done
library_hash_after="$(cd "$app_dir/lib" && rg --files src | LC_ALL=C sort \
  | xargs sha256sum | sha256sum | cut -d' ' -f1)"
if [[ "$library_hash_before" != "$library_hash_after" ]]; then
  echo "Generated library changed during the application probe" >&2
  exit 1
fi
printf 'GENERATED_EXACT_DECIMAL_GATE_PASS rounds=2 workspace=2 library_unchanged=1 generator=%s runtime=%s evidence=%s\n' \
  "$(git -C "$generator_root" rev-parse --short HEAD)" \
  "$(git -C "$runtime_root" rev-parse --short HEAD)" "$run_dir"
