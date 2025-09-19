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

echo "Will tag and push: $TAG"
read -r -p "Continue? (y/N): " CONFIRM
if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
  echo "Aborted"
  exit 1
fi

git tag -a "$TAG" -m "Release $TAG"
git push
git push origin "$TAG"

echo "Done. Create a GitHub release from tag $TAG (or run scripts/release.sh)"
