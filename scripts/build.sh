#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if ! command -v mvn >/dev/null 2>&1; then
  echo "[ERROR] Maven (mvn) not found in PATH" >&2
  exit 1
fi

echo "Building plugin (skip tests)..."
mvn -q -DskipTests package

echo "Done. Artifacts in target/"
