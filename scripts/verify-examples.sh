#!/usr/bin/env bash
set -euo pipefail

repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
expected=(conformance order-management school-management)
mapfile -t actual < <(find "$repo/examples" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort)
if [[ "${actual[*]}" != "${expected[*]}" ]]; then
  echo "example inventory changed; update scripts/verify-examples.sh: ${actual[*]}" >&2
  exit 1
fi

cd "$repo"
mvn -q -DskipTests install
mvn -q -f examples/conformance/lib/pom.xml install -DskipTests
mvn -q -f examples/conformance/pom.xml spring-boot:run -Dspring-boot.run.arguments=--spring.main.web-application-type=none
mvn -q -f examples/school-management/lib/pom.xml install -DskipTests
mvn -q -f examples/school-management/pom.xml spring-boot:run -Dspring-boot.run.arguments=--spring.main.web-application-type=none
mvn -q -f examples/order-management/pom.xml install -DskipTests
mvn -q -f examples/order-management/pom.xml exec:java -pl java-app-console
echo "PASS: all Java examples"
