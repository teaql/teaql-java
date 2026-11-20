#!/usr/bin/env bash
set -euo pipefail
example_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
runtime_dir="$(cd "$example_dir/../.." && pwd)"
reactor_dir="$(mktemp -d -t teaql-school-projection-reactor.XXXXXXXX)"
trap 'rm -rf -- "$reactor_dir"' EXIT
python3 -B - "$reactor_dir/pom.xml" "$runtime_dir" "$example_dir/java-lib-core/lib" "$example_dir/java-app-console" <<'PY'
from pathlib import Path
import sys
from xml.etree.ElementTree import Element, SubElement, ElementTree

pom = Element('project', xmlns='http://maven.apache.org/POM/4.0.0')
for key, value in (
    ('modelVersion', '4.0.0'), ('groupId', 'io.teaql.test'),
    ('artifactId', 'school-record-projection-reactor'), ('version', '1'),
    ('packaging', 'pom'),
):
    SubElement(pom, key).text = value
modules = SubElement(pom, 'modules')
for module in sys.argv[2:]:
    SubElement(modules, 'module').text = module
ElementTree(pom).write(Path(sys.argv[1]), encoding='utf-8', xml_declaration=True)
PY
mvn -q -f "$reactor_dir/pom.xml" -pl :school-record-projection-workspace -am "$@"
