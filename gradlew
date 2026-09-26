#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION=8.11.1
CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/ruru-bootstrap/gradle-$GRADLE_VERSION"
GRADLE_BIN="$CACHE_DIR/bin/gradle"
if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$(dirname "$CACHE_DIR")"
  TMP="${TMPDIR:-/tmp}/gradle-$GRADLE_VERSION.zip"
  URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$URL" -o "$TMP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$TMP" "$URL"
  else
    echo "需要 curl 或 wget 来首次下载 Gradle $GRADLE_VERSION。" >&2
    exit 1
  fi
  rm -rf "$CACHE_DIR"
  mkdir -p "$(dirname "$CACHE_DIR")"
  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$TMP" -d "$(dirname "$CACHE_DIR")"
  else
    echo "需要 unzip 来解压 Gradle。" >&2
    exit 1
  fi
  rm -f "$TMP"
fi
exec "$GRADLE_BIN" -p "$ROOT" "$@"
