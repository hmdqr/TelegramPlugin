#!/usr/bin/env bash
set -euo pipefail

# Interactive publisher for Linux/macOS
# - Gathers answers first, Ctrl+C at any time before confirmation cancels safely
# - Then performs: optional version bump (+commit/push), build, tag, push, optional GitHub Release

cd "$(dirname "$0")/.."

trap 'echo; echo "[ABORT] Cancelled by user."; exit 130' INT

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[ERROR] Required command not found: $1" >&2
    exit 1
  fi
}

ask_yes_no() {
  local prompt="$1"; shift
  local def="${1:-y}"; shift || true
  local opts="[Y/n]"; [[ "$def" == "n" ]] && opts="[y/N]"
  while true; do
    read -r -p "$prompt $opts " ans || true
    if [[ -z "$ans" ]]; then
      [[ "$def" == "y" ]] && return 0 || return 1
    fi
    case "$ans" in
      y|Y|yes|YES) return 0;;
      n|N|no|NO) return 1;;
      *) echo "Please answer y or n.";;
    esac
  done
}

ask_value() {
  local prompt="$1"; shift
  local def="${1:-}"; shift || true
  if [[ -n "$def" ]]; then
    read -r -p "$prompt [$def]: " val || true
    echo "${val:-$def}"
  else
    read -r -p "$prompt: " val || true
    echo "$val"
  fi
}

suggest_next_version() {
  local v="$1"
  if [[ "$v" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)(.*)$ ]]; then
    local major="${BASH_REMATCH[1]}" minor="${BASH_REMATCH[2]}" patch="${BASH_REMATCH[3]}" rest="${BASH_REMATCH[4]}"
    if [[ -z "$rest" ]]; then
      echo "$major.$minor.$((patch+1))"
      return 0
    fi
  fi
  echo ""
}

require_cmd mvn
require_cmd git

HAS_GH=0
if command -v gh >/dev/null 2>&1; then HAS_GH=1; fi

CURRENT_VERSION=$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version 2>/dev/null | sed -n '/^[0-9]/p' | head -n 1 | tr -d '\r' | xargs || true)
CURRENT_VERSION=${CURRENT_VERSION:-unknown}
SUGGESTED_NEXT=$(suggest_next_version "$CURRENT_VERSION")
BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")

echo "============================="
echo "TelegramPlugin Publisher"
echo "Repo: $(basename "$PWD") — Branch: $BRANCH"
echo "Current version: $CURRENT_VERSION"
echo "============================="

# Check working tree cleanliness
if [[ -n "$(git status --porcelain)" ]]; then
  echo "Your working tree has uncommitted changes:"
  git status --short
  if ! ask_yes_no "Proceed anyway?" n; then
    echo "Please commit/stash changes and re-run."
    exit 1
  fi
fi

DO_BUMP=no
NEW_VERSION=""
if ask_yes_no "Bump project version?" y; then
  DO_BUMP=yes
  if [[ -z "$SUGGESTED_NEXT" ]]; then
    NEW_VERSION=$(ask_value "Enter new version" "")
  else
    NEW_VERSION=$(ask_value "Enter new version" "$SUGGESTED_NEXT")
  fi
  if [[ -z "$NEW_VERSION" ]]; then
    echo "[ERROR] No version provided."
    exit 1
  fi
fi

DO_COMMIT=no
DO_PUSH=no
if [[ "$DO_BUMP" == "yes" ]]; then
  if ask_yes_no "Commit version bump?" y; then DO_COMMIT=yes; fi
  if [[ "$DO_COMMIT" == "yes" ]] && ask_yes_no "Push commit to origin/$BRANCH?" y; then DO_PUSH=yes; fi
fi

DO_BUILD=no
if ask_yes_no "Build shaded JAR now?" y; then DO_BUILD=yes; fi

TAG_NAME=""
DO_TAG=no
if ask_yes_no "Create git tag?" y; then
  DO_TAG=yes
  local_v="$CURRENT_VERSION"
  if [[ "$DO_BUMP" == "yes" ]]; then local_v="$NEW_VERSION"; fi
  TAG_NAME=$(ask_value "Tag name" "v$local_v")
fi

DO_PUSH_TAG=no
if [[ "$DO_TAG" == "yes" ]] && ask_yes_no "Push tag to origin?" y; then DO_PUSH_TAG=yes; fi

DO_RELEASE=no
RELEASE_TITLE=""
RELEASE_NOTES=""
USE_EDITOR=no
if [[ "$HAS_GH" -eq 1 ]] && [[ "$DO_TAG" == "yes" ]]; then
  if ask_yes_no "Create GitHub Release with gh?" y; then
    DO_RELEASE=yes
    RELEASE_TITLE=$(ask_value "Release title" "$TAG_NAME")
    if ask_yes_no "Open editor for release notes?" n; then
      USE_EDITOR=yes
    else
      RELEASE_NOTES=$(ask_value "Short release notes" "Automated release")
    fi
  fi
fi

echo
echo "======== Plan Summary ========"
echo "Branch: $BRANCH"
if [[ "$DO_BUMP" == "yes" ]]; then
  echo "- Bump version: $CURRENT_VERSION -> $NEW_VERSION"
  [[ "$DO_COMMIT" == "yes" ]] && echo "- Commit version bump" || echo "- Do NOT commit bump"
  [[ "$DO_PUSH" == "yes" ]] && echo "- Push commit to origin/$BRANCH" || echo "- Do NOT push commit"
else
  echo "- Keep version: $CURRENT_VERSION"
fi
[[ "$DO_BUILD" == "yes" ]] && echo "- Build shaded JAR" || echo "- Skip build"
if [[ "$DO_TAG" == "yes" ]]; then
  echo "- Create tag: $TAG_NAME"
  [[ "$DO_PUSH_TAG" == "yes" ]] && echo "- Push tag to origin" || echo "- Do NOT push tag"
fi
if [[ "$DO_RELEASE" == "yes" ]]; then
  echo "- Create GitHub Release: $TAG_NAME (title: $RELEASE_TITLE)"
  if [[ "$USE_EDITOR" == "yes" ]]; then echo "- Notes: from editor"; else echo "- Notes: $RELEASE_NOTES"; fi
fi
echo "=============================="

if ! ask_yes_no "Proceed?" y; then
  echo "No changes made. Bye."
  exit 0
fi

# Execute plan
if [[ "$DO_BUMP" == "yes" ]]; then
  echo "[STEP] Setting project version to $NEW_VERSION ..."
  mvn -q versions:set -DnewVersion="$NEW_VERSION" -DgenerateBackupPoms=false
  if [[ "$DO_COMMIT" == "yes" ]]; then
    git add pom.xml
    git commit -m "chore: bump version to $NEW_VERSION"
    if [[ "$DO_PUSH" == "yes" ]]; then
      git push origin "$BRANCH"
    fi
  fi
fi

if [[ "$DO_BUILD" == "yes" ]]; then
  echo "[STEP] Building shaded JAR ..."
  mvn -q -DskipTests package
fi

JAR_PATH=$(ls -1 target/*-shaded.jar 2>/dev/null | head -n 1 || true)
if [[ "$DO_BUILD" == "yes" ]] && [[ -z "$JAR_PATH" ]]; then
  echo "[WARN] No shaded JAR found in target/. Continuing."
fi

if [[ "$DO_TAG" == "yes" ]]; then
  echo "[STEP] Creating tag $TAG_NAME ..."
  if git rev-parse -q --verify "$TAG_NAME" >/dev/null 2>&1; then
    echo "[ERROR] Tag already exists locally: $TAG_NAME"
    exit 1
  fi
  git tag -a "$TAG_NAME" -m "Release $TAG_NAME"
  if [[ "$DO_PUSH_TAG" == "yes" ]]; then
    echo "[STEP] Pushing tag $TAG_NAME ..."
    if ! git push origin "$TAG_NAME"; then
      echo "[ERROR] Failed to push tag. Deleting local tag." >&2
      git tag -d "$TAG_NAME" || true
      exit 1
    fi
  fi
fi

if [[ "$DO_RELEASE" == "yes" ]]; then
  echo "[STEP] Creating GitHub Release $TAG_NAME ..."
  if [[ "$USE_EDITOR" == "yes" ]]; then
    gh release create "$TAG_NAME" ${JAR_PATH:+"$JAR_PATH"} --title "$RELEASE_TITLE"
  else
    gh release create "$TAG_NAME" ${JAR_PATH:+"$JAR_PATH"} --title "$RELEASE_TITLE" --notes "$RELEASE_NOTES"
  fi
fi

echo "All done."