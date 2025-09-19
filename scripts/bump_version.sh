#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if ! command -v mvn >/dev/null 2>&1; then
  echo "[ERROR] Maven (mvn) not found in PATH" >&2
  exit 1
fi

NEW_VERSION="${1:-}"
if [[ -z "$NEW_VERSION" ]]; then
  read -r -p "Enter new version (e.g., 1.1.0): " NEW_VERSION
fi
if [[ -z "$NEW_VERSION" ]]; then
  echo "[ERROR] No version provided" >&2
  exit 1
fi

echo "Setting project version to $NEW_VERSION ..."
mvn -q versions:set -DnewVersion="$NEW_VERSION" -DgenerateBackupPoms=false

CURR_VERSION=$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version)
echo "Version is now $CURR_VERSION"
