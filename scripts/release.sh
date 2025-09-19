#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

for cmd in mvn git; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "[ERROR] $cmd not found in PATH" >&2
    exit 1
  fi
done

VERSION=$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version)
if [[ -z "$VERSION" ]]; then
  echo "[ERROR] Unable to read project version" >&2
  exit 1
fi
TAG="v$VERSION"

echo "Building for release ..."
mvn -q -DskipTests package

JAR=$(ls -1 target/*-shaded.jar 2>/dev/null | head -n 1 || true)
if [[ -z "$JAR" ]]; then
  echo "[ERROR] Could not find shaded jar in target/" >&2
  exit 1
fi

git tag -a "$TAG" -m "Release $TAG"
git push
git push origin "$TAG"

if command -v gh >/dev/null 2>&1; then
  echo "Creating GitHub release $TAG ..."
  gh release create "$TAG" "$JAR" --title "$TAG" --notes "Automated release"
else
  echo "'gh' not found. Publish the tag as a GitHub Release in the UI; the workflow will upload the JAR."
fi

echo "Done."
