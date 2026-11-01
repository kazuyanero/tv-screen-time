#!/bin/sh
# Install Screen Time on an Android TV over ADB and set it up in one go:
# install the APK, grant its permissions, set the parent PIN and the daily limit, start it.
#
# Usage (from the repo folder, USB/network debugging on, TV and computer on the same network):
#   scripts/setup.sh <tv ip> <4-digit pin> [limit minutes, default 30] [apk path]
#
# Needs Google's adb (Android SDK platform-tools) on PATH.
set -eu

if [ $# -lt 2 ]; then
    echo "usage: $0 <tv ip> <4-digit pin> [limit minutes] [apk]" >&2
    exit 2
fi
host=$1
pin=$2
limit=${3:-30}
apk=${4:-app/build/outputs/apk/release/app-release.apk}
pkg=local.tv.screentime

case $pin in
    [0-9][0-9][0-9][0-9]) ;;
    *) echo "the PIN must be exactly 4 digits" >&2; exit 2 ;;
esac
command -v adb >/dev/null || { echo "adb not found: install Android SDK platform-tools" >&2; exit 2; }
[ -f "$apk" ] || { echo "APK not found: $apk (build it first: ./gradlew assembleRelease)" >&2; exit 2; }
case $host in *:*) target=$host ;; *) target=$host:5555 ;; esac

echo "Connecting to $target (accept 'Allow USB debugging?' on the TV the first time)"
adb connect "$target"
adb -s "$target" wait-for-device

echo "Installing"
adb -s "$target" install -r "$apk"

echo "Granting permissions"
adb -s "$target" shell appops set $pkg GET_USAGE_STATS allow
adb -s "$target" shell appops set $pkg SYSTEM_ALERT_WINDOW allow
# TCL and some other brands block apps from starting at boot unless they hold this app-op; others do not have it.
adb -s "$target" shell appops set $pkg APP_AUTO_START allow 2>/dev/null || true
adb -s "$target" shell appops write-settings

echo "Setting the PIN and a daily limit of $limit minutes"
adb -s "$target" shell am broadcast -n "$pkg/.AdminReceiver" --es pin "$pin" --ei limit "$limit"

echo "Checking"
adb -s "$target" shell "appops get $pkg | grep -E 'USAGE|ALERT|AUTO_START'"
adb -s "$target" shell "dumpsys activity services $pkg | grep -m1 ServiceRecord"
echo "Done. Open 'Screen Time' on the TV and enter the PIN to see its settings."
