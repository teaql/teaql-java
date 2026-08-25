#!/usr/bin/env bash
set -euo pipefail

version="${1:?usage: verify_maven_release.sh VERSION}"
repository="${TEAQL_MAVEN_RELEASE_REPOSITORY:-https://maven.teaql.io/repository/maven-releases}"

retry_head() {
  local url="$1"
  local attempt
  for attempt in 1 2 3 4 5; do
    if curl --fail --silent --show-error --head "$url" >/dev/null; then
      return 0
    fi
    if [[ "$attempt" -lt 5 ]]; then
      sleep 3
    fi
  done
  echo "Missing release artifact: $url" >&2
  return 1
}

metadata="$repository/io/teaql/teaql-core/maven-metadata.xml"
for attempt in 1 2 3 4 5; do
  if curl --fail --silent --show-error "$metadata" | grep -Fq "<release>$version</release>"; then
    break
  fi
  if [[ "$attempt" -eq 5 ]]; then
    echo "teaql-core metadata does not publish $version as the release" >&2
    exit 1
  fi
  sleep 3
done

mapfile -t modules < <(
  sed -n '/<modules>/,/<\/modules>/s:.*<module>\([^<]*\)</module>.*:\1:p' pom.xml \
    | grep -v '^examples/'
)
if [[ "${#modules[@]}" -ne 33 ]]; then
  echo "Expected 33 child modules, found ${#modules[@]}" >&2
  exit 1
fi

retry_head "$repository/io/teaql/teaql-java-parent/$version/teaql-java-parent-$version.pom"
for module in "${modules[@]}"; do
  artifact="$(basename "$module")"
  base="$repository/io/teaql/$artifact/$version/$artifact-$version"
  retry_head "$base.pom"
  retry_head "$base.jar"
  retry_head "$base-sources.jar"
done

echo "Verified TeaQL Java $version: parent plus ${#modules[@]} child modules"
