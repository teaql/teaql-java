#!/usr/bin/env bash
set -euo pipefail

repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
verification_dir="$(mktemp -d)"
trap 'rm -rf -- "$verification_dir"' EXIT
expected=(conformance order-management school-management)
mapfile -t actual < <(find "$repo/examples" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort)
if [[ "${actual[*]}" != "${expected[*]}" ]]; then
  echo "example inventory changed; update scripts/verify-examples.sh: ${actual[*]}" >&2
  exit 1
fi

cd "$repo"
mvn -q -DskipTests install
mvn -q -f examples/conformance/lib/pom.xml install -DskipTests
mvn -q -f examples/conformance/pom.xml spring-boot:run \
  -Dspring-boot.run.arguments="--spring.main.web-application-type=none --spring.datasource.url=jdbc:sqlite:$verification_dir/conformance.db"
mvn -q -f examples/school-management/lib/pom.xml install -DskipTests
mvn -q -f examples/school-management/pom.xml spring-boot:run \
  -Dspring-boot.run.arguments="--spring.main.web-application-type=none --spring.datasource.url=jdbc:sqlite:$verification_dir/school-management.db"
mvn -q -f examples/order-management/pom.xml install -DskipTests
mvn -q -f examples/order-management/pom.xml exec:java -pl java-app-console
echo "PASS: all Java examples"
