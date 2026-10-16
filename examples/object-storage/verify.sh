#!/usr/bin/env bash
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
: "${TEAQL_S3_ENDPOINT:?dedicated local test endpoint required}"
: "${TEAQL_S3_BUCKET:?dedicated test bucket required}"
: "${TEAQL_S3_ACCESS_KEY:?test access key required}"
: "${TEAQL_S3_SECRET_KEY:?test secret key required}"
cd "$repo"
mvn -B -pl examples/object-storage -am -Dteaql.s3.live=true -Dteaql.s3.iterations="${TEAQL_S3_ITERATIONS:-1}" \
  -Dtest=ObjectStorageExampleTest,ObjectStorageContractTest,S3ObjectStorageTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
