#!/usr/bin/env bash
set -euo pipefail
example="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$example/../.." && pwd)"
: "${TEAQL_S3_ENDPOINT:?dedicated local test endpoint required}"
: "${TEAQL_S3_BUCKET:?dedicated test bucket required}"
: "${TEAQL_S3_ACCESS_KEY:?test access key required}"
: "${TEAQL_S3_SECRET_KEY:?test secret key required}"
: "${TEAQL_MAVEN_DEPENDENCY_CACHE:?Use the documented isolated Maven qualifier}"
if [[ -n "${TEAQL_ORDER_ATTACHMENT_EVIDENCE_DIR:-}" ]]; then
  evidence="$TEAQL_ORDER_ATTACHMENT_EVIDENCE_DIR"
  mkdir -p "$(dirname "$evidence")"
  mkdir "$evidence"
else
  evidence="$(mktemp -d -t teaql-java-order-attachment.XXXXXXXX)"
fi
evidence="$(cd "$evidence" && pwd)"
scratch="$(mktemp -d -t teaql-java-order-attachment-build.XXXXXXXX)"
trap 'status=$?; trap - EXIT; rm -rf -- "$scratch"; exit "$status"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
fingerprint() {
  (cd "$example" && find workspace-pom.xml java-lib-core java-app-console -type f -not -path '*/target/*' -print0 | sort -z | xargs -0 sha256sum)
}
fingerprint > "$evidence/source-before.sha256"
cd "$repo"
mvn -o -B -pl examples/object-storage,teaql-sqlite,teaql-provider-jdbc -am -Dteaql.s3.live=true \
  -Dteaql.s3.iterations="${TEAQL_S3_ITERATIONS:-1}" \
  -Dtest=ObjectStorageExampleTest,ObjectStorageContractTest,S3ObjectStorageTest \
  -Dsurefire.failIfNoSpecifiedTests=false install 2>&1 | tee "$evidence/s3-calibration.log"
cp "$example/workspace-pom.xml" "$scratch/pom.xml"
cp -R "$example/java-lib-core" "$example/java-app-console" "$scratch/"
mvn -o -f "$scratch/pom.xml" -DskipTests install > "$evidence/workspace-build.log" 2>&1
jar="$scratch/java-lib-core/lib/target/order-attachment-service-core-0.0.1-SNAPSHOT.jar"
installed="$TEAQL_MAVEN_DEPENDENCY_CACHE/com/teaql/order-attachment-service-core/0.0.1-SNAPSHOT/${jar##*/}"
cmp "$jar" "$installed"
sha256sum "$installed" > "$evidence/domain-artifact.sha256"
for round in 1 2; do
  TEAQL_ATTACHMENT_DB="$evidence/orders.sqlite" TEAQL_ATTACHMENT_DOWNLOAD="$evidence/receipt-$round.txt" TEAQL_ATTACHMENT_ROUND="$round" \
    mvn -o -f "$scratch/pom.xml" -pl java-app-console exec:java 2>&1 | tee "$evidence/run-$round.log"
done
rg '^(ATTACHMENT_ORIGIN|S3_PROVIDER_ORIGIN) ' "$evidence/run-1.log" > "$evidence/origins.log"
fingerprint > "$evidence/source-after.sha256"
cmp "$evidence/source-before.sha256" "$evidence/source-after.sha256"
printf 'Order attachment evidence retained at %s\n' "$evidence"
