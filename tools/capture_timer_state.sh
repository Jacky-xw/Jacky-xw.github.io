#!/bin/sh
# Run on a computer with adb and USB debugging authorized, BEFORE reopening the app.
# Observation only. Does not wake, launch, stop or change the application.
# Dumps may contain other apps' system metadata; review before sharing.
set -eu
out="${1:-timer-evidence-$(date +%Y%m%d-%H%M%S)}"
mkdir -p "$out"
adb get-state > "$out/device-state.txt"
adb shell date > "$out/device-time.txt"
adb shell dumpsys alarm > "$out/alarm.txt"
adb shell dumpsys activity services com.ruru.practice > "$out/services.txt"
adb shell dumpsys activity processes > "$out/processes.txt"
adb shell dumpsys power > "$out/power.txt"
adb shell dumpsys deviceidle > "$out/deviceidle.txt"
adb shell dumpsys package com.ruru.practice > "$out/package.txt"
adb logcat -d -v threadtime -s PracticeTimer > "$out/timer-logcat.txt"
printf 'Saved local evidence in %s. Review system metadata before sharing.\n' "$out"
