#!/usr/bin/env bash
#
# Build the debug APK and deploy it to a connected Android device or emulator.
#
# Usage:
#   scripts/deploy-debug.sh              # auto-select the only connected device
#   scripts/deploy-debug.sh <serial>     # deploy to a specific device (see 'adb devices')
#   scripts/deploy-debug.sh --no-launch  # install without starting the app
#
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_ID="com.usharik.app"
MAIN_ACTIVITY="com.usharik.app.MainActivity"
APK="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

# --- Parse arguments --------------------------------------------------------
SERIAL=""
LAUNCH=1
for arg in "$@"; do
    case "$arg" in
        --no-launch) LAUNCH=0 ;;
        -h|--help)   grep '^#' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *)           SERIAL="$arg" ;;
    esac
done

# --- Locate adb (local.properties -> ANDROID_HOME -> PATH -> default) -------
SDK_DIR=""
if [[ -f "$PROJECT_DIR/local.properties" ]]; then
    SDK_DIR="$(sed -n 's/^sdk\.dir=//p' "$PROJECT_DIR/local.properties" | head -1)"
fi
SDK_DIR="${SDK_DIR:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
ADB="$SDK_DIR/platform-tools/adb"
[[ -x "$ADB" ]] || ADB="$(command -v adb || true)"
if [[ -z "$ADB" ]]; then
    echo "ERROR: adb not found (checked local.properties sdk.dir, ANDROID_HOME, PATH)" >&2
    exit 1
fi

# --- Pick a device -----------------------------------------------------------
DEVICES=()
while IFS= read -r line; do
    DEVICES+=("$line")
done < <("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}')

if [[ ${#DEVICES[@]} -eq 0 ]]; then
    echo "ERROR: no devices/emulators connected. Output of 'adb devices':" >&2
    "$ADB" devices >&2
    exit 1
fi

if [[ -n "$SERIAL" ]]; then
    if ! printf '%s\n' "${DEVICES[@]}" | grep -qx "$SERIAL"; then
        echo "ERROR: device '$SERIAL' not found. Connected devices: ${DEVICES[*]}" >&2
        exit 1
    fi
elif [[ ${#DEVICES[@]} -eq 1 ]]; then
    SERIAL="${DEVICES[0]}"
else
    echo "Multiple devices connected - pass a serial as argument:" >&2
    "$ADB" devices -l >&2
    exit 1
fi

MODEL="$("$ADB" -s "$SERIAL" shell getprop ro.product.model 2>/dev/null | tr -d '\r')"
echo "==> Target device: $SERIAL (${MODEL:-unknown model})"

# --- Build -------------------------------------------------------------------
echo "==> Building debug APK..."
"$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" :app:assembleDebug

if [[ ! -f "$APK" ]]; then
    echo "ERROR: APK not found at $APK" >&2
    exit 1
fi

# --- Install -----------------------------------------------------------------
echo "==> Installing $(basename "$APK")..."
"$ADB" -s "$SERIAL" install -r "$APK"

# --- Launch ------------------------------------------------------------------
if [[ $LAUNCH -eq 1 ]]; then
    echo "==> Launching $APP_ID..."
    "$ADB" -s "$SERIAL" shell am start -n "$APP_ID/$MAIN_ACTIVITY"
fi

echo "==> Done."
