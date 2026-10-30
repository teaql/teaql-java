#!/usr/bin/env bash
set -euo pipefail

version="${1:?usage: verify_maven_release_absence.sh VERSION}"
repository="${TEAQL_MAVEN_RELEASE_REPOSITORY:-https://maven.teaql.io/repository/maven-releases}"
repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"

require_absent() {
  local url="$1"
  local attempt status
  for attempt in 1 2 3 4 5; do
    status="$(curl --silent --show-error --location --max-time 30 \
      --output /dev/null --write-out '%{http_code}' "$url")" || status=000
    case "$status" in
      404) return 0 ;;
      200) echo "Release artifact already exists: $url" >&2; return 1 ;;
    esac
    if [[ "$attempt" -lt 5 ]]; then
      sleep 3
    fi
  done
  echo "Could not prove release artifact is absent ($status): $url" >&2
  return 1
}

mapfile -t modules < <(
  sed -n '/<modules>/,/<\/modules>/s:.*<module>\([^<]*\)</module>.*:\1:p' "$repo_dir/pom.xml" \
    | grep -v '^examples/'
)
if [[ "${#modules[@]}" -eq 0 ]]; then
  echo "No release child modules found in $repo_dir/pom.xml" >&2
  exit 1
fi

require_absent "$repository/io/teaql/teaql-java-parent/$version/teaql-java-parent-$version.pom"
for module in "${modules[@]}"; do
  artifact="$(basename "$module")"
  base="$repository/io/teaql/$artifact/$version/$artifact-$version"
  for suffix in .pom .jar -sources.jar; do
    require_absent "$base$suffix"
  done
done

echo "Confirmed TeaQL Java $version has no release assets: parent plus ${#modules[@]} child modules"
