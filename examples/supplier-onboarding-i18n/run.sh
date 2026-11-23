#!/usr/bin/env bash
set -euo pipefail

example="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$example/../.." && pwd)"
runtime="${TEAQL_JAVA_EXAMPLE_SOURCE_ROOT:-$repo}"
scratch="$(mktemp -d -t teaql-supplier-java.XXXXXXXX)"
cleanup() {
  local status=$?
  trap - EXIT
  python3 -c 'import shutil, sys; shutil.rmtree(sys.argv[1])' "$scratch"
  exit "$status"
}
trap cleanup EXIT
cp -R -- "$example/java-app-console" "$example/java-lib-core" "$scratch/"
python3 - "$runtime" "$scratch/runtime" <<'PY'
import os
from pathlib import Path
import shutil
import subprocess
import sys

source, destination = map(Path, sys.argv[1:])
files = subprocess.check_output(['git', '-C', source, 'ls-files', '-z', '--cached', '--others', '--exclude-standard'])
for name in set(os.fsdecode(files).split('\0')) - {''}:
    path, target = source / name, destination / name
    if path.is_file():
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, target)
PY
cat > "$scratch/settings.xml" <<'XML'
<settings><mirrors><mirror><id>central</id><mirrorOf>*</mirrorOf><url>https://repo.maven.apache.org/maven2</url></mirror></mirrors></settings>
XML
export MAVEN_SKIP_RC=1
unset MAVEN_OPTS MAVEN_ARGS
maven=("${MAVEN:-mvn}" -B -ntp -s "$scratch/settings.xml" -gs "$scratch/settings.xml"
       "-Dmaven.repo.local=$scratch/maven" "-Djava.io.tmpdir=$scratch")
cd -- "$scratch"
"${maven[@]}" -f "$scratch/runtime/pom.xml" -pl teaql-sqlite -am -DskipTests install >&2
"${maven[@]}" -f "$scratch/java-lib-core/lib/pom.xml" -DskipTests install >&2
"${maven[@]}" -f "$scratch/java-app-console/pom.xml" -DskipTests package >&2
driver=(python3 "$example/run-example.py" --catalog "$example/java-app-console/teaql-i18n.json")
binary=("${JAVA_HOME:+$JAVA_HOME/bin/}java" "-Djava.io.tmpdir=$scratch" -jar
        "$scratch/java-app-console/target/supplier-onboarding-service-0.0.1-SNAPSHOT.jar")
if [[ "${1:-}" == --verify ]]; then
  "${driver[@]}" --all --repeat 2 -- "${binary[@]}"
  "${driver[@]}" --edge-cases -- "${binary[@]}"
else
  "${driver[@]}" "$@" -- "${binary[@]}"
fi
