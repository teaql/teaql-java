#!/usr/bin/env bash
set -euo pipefail

version="${1:?usage: verify_maven_release.sh VERSION}"
repository="${TEAQL_MAVEN_RELEASE_REPOSITORY:-https://maven.teaql.io/repository/maven-releases}"
repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
scratch="$(mktemp -d)"
trap 'rm -rf -- "$scratch"' EXIT

retry_matching_artifact() {
  local url="$1"
  local source="$2"
  local attempt mismatch=0
  if [[ ! -f "$source" ]]; then
    echo "Missing local release artifact: $source" >&2
    return 1
  fi
  for attempt in 1 2 3 4 5; do
    if curl --fail --silent --show-error --location --max-time 30 \
        --output "$scratch/download" "$url"; then
      if cmp --silent -- "$source" "$scratch/download"; then
        return 0
      fi
      mismatch=1
    fi
    if [[ "$attempt" -lt 5 ]]; then
      sleep 3
    fi
  done
  if [[ "$mismatch" -eq 1 ]]; then
    echo "Release artifact byte mismatch: $url (local: $source)" >&2
  else
    echo "Missing release artifact: $url" >&2
  fi
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
  sed -n '/<modules>/,/<\/modules>/s:.*<module>\([^<]*\)</module>.*:\1:p' "$repo_dir/pom.xml" \
    | grep -v '^examples/'
)
if [[ "${#modules[@]}" -eq 0 ]]; then
  echo "No release child modules found in $repo_dir/pom.xml" >&2
  exit 1
fi

retry_matching_artifact \
  "$repository/io/teaql/teaql-java-parent/$version/teaql-java-parent-$version.pom" \
  "$repo_dir/pom.xml"
for module in "${modules[@]}"; do
  artifact="$(basename "$module")"
  base="$repository/io/teaql/$artifact/$version/$artifact-$version"
  retry_matching_artifact "$base.pom" "$repo_dir/$module/pom.xml"
  retry_matching_artifact "$base.jar" "$repo_dir/$module/target/$artifact-$version.jar"
  retry_matching_artifact "$base-sources.jar" \
    "$repo_dir/$module/target/$artifact-$version-sources.jar"
done

echo "Verified TeaQL Java $version bytes: parent plus ${#modules[@]} child modules"
