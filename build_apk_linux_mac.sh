#!/usr/bin/env sh
set -eu
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle not found. Open the project in Android Studio or install Gradle + Android SDK." >&2
  exit 1
fi
gradle :app:assembleDebug
echo "APK: app/build/outputs/apk/debug/app-debug.apk"
