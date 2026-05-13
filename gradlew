#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROPS_FILE="$ROOT_DIR/gradle/wrapper/gradle-wrapper.properties"
DIST_URL="$(grep '^distributionUrl=' "$PROPS_FILE" | cut -d= -f2-)"
DIST_URL="${DIST_URL//\\:/:}"
DIST_NAME="${DIST_URL##*/}"
GRADLE_USER_HOME_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}"
CACHE_DIR="$GRADLE_USER_HOME_DIR/wrapper/dists/${DIST_NAME%.zip}"
ZIP_FILE="$CACHE_DIR/$DIST_NAME"
EXTRACT_DIR="$CACHE_DIR/unpacked"
GRADLE_DIR="$EXTRACT_DIR/gradle-8.10.2"

if [[ ! -x "$GRADLE_DIR/bin/gradle" ]]; then
  mkdir -p "$CACHE_DIR" "$EXTRACT_DIR"
  if [[ ! -f "$ZIP_FILE" ]]; then
    curl -fsSL "$DIST_URL" -o "$ZIP_FILE"
  fi
  unzip -q -o "$ZIP_FILE" -d "$EXTRACT_DIR"
fi

exec "$GRADLE_DIR/bin/gradle" "$@"
