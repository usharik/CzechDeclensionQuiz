#!/bin/bash

# Start Android Emulator Script
# This script starts the Android emulator and waits for it to be ready

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
EMULATOR_NAME="${1:-test_emulator}"
TIMEOUT=300  # 5 minutes timeout

# Function to print colored output
print_status() {
    if [ "$1" = "success" ]; then
        echo -e "${GREEN}✓${NC} $2"
    elif [ "$1" = "error" ]; then
        echo -e "${RED}✗${NC} $2"
    elif [ "$1" = "warning" ]; then
        echo -e "${YELLOW}⚠${NC} $2"
    elif [ "$1" = "info" ]; then
        echo -e "${BLUE}ℹ${NC} $2"
    else
        echo "$2"
    fi
}

# Function to print section header
print_header() {
    echo ""
    echo "========================================="
    echo "$1"
    echo "========================================="
    echo ""
}

print_header "Starting Android Emulator"

# Set up environment
export ANDROID_HOME
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

# Check if emulator command exists
if ! command -v emulator &> /dev/null; then
    print_status "error" "Emulator not found. Please run setup-android-emulator.sh first"
    exit 1
fi

# Check if ADB exists
if ! command -v adb &> /dev/null; then
    print_status "error" "ADB not found. Please run setup-android-emulator.sh first"
    exit 1
fi

# Check if emulator exists
if ! "$ANDROID_HOME/emulator/emulator" -list-avds | grep -q "^${EMULATOR_NAME}$"; then
    print_status "error" "Emulator '$EMULATOR_NAME' not found"
    echo ""
    echo "Available emulators:"
    "$ANDROID_HOME/emulator/emulator" -list-avds
    echo ""
    echo "Create one with: ./setup-android-emulator.sh"
    exit 1
fi

# Check if emulator is already running
if adb devices | grep -q "emulator"; then
    print_status "warning" "An emulator is already running"
    adb devices
    echo ""
    read -p "Do you want to continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 0
    fi
fi

print_status "info" "Starting emulator: $EMULATOR_NAME"
print_status "info" "This may take a few minutes..."

# Start emulator in background
"$ANDROID_HOME/emulator/emulator" -avd "$EMULATOR_NAME" \
    -no-snapshot-load \
    -no-boot-anim \
    -gpu auto \
    > /tmp/emulator.log 2>&1 &

EMULATOR_PID=$!
print_status "info" "Emulator process started (PID: $EMULATOR_PID)"

# Wait for emulator to appear in adb devices
print_status "info" "Waiting for emulator to appear..."
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
    if adb devices | grep -q "emulator.*device"; then
        break
    fi
    sleep 2
    ELAPSED=$((ELAPSED + 2))
    echo -n "."
done
echo ""

if [ $ELAPSED -ge $TIMEOUT ]; then
    print_status "error" "Timeout waiting for emulator to start"
    print_status "info" "Check logs at: /tmp/emulator.log"
    exit 1
fi

# Get emulator serial
EMULATOR_SERIAL=$(adb devices | grep "emulator" | awk '{print $1}' | head -1)
print_status "success" "Emulator appeared: $EMULATOR_SERIAL"

# Wait for emulator to be fully booted
print_status "info" "Waiting for emulator to fully boot..."
adb -s "$EMULATOR_SERIAL" wait-for-device

ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
    BOOT_COMPLETE=$(adb -s "$EMULATOR_SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
    if [ "$BOOT_COMPLETE" = "1" ]; then
        break
    fi
    sleep 2
    ELAPSED=$((ELAPSED + 2))
    echo -n "."
done
echo ""

if [ $ELAPSED -ge $TIMEOUT ]; then
    print_status "error" "Timeout waiting for emulator to boot"
    exit 1
fi

print_status "success" "Emulator is fully booted and ready!"

# Show emulator info
print_header "Emulator Information"
echo "Serial: $EMULATOR_SERIAL"
echo "Android Version: $(adb -s "$EMULATOR_SERIAL" shell getprop ro.build.version.release | tr -d '\r')"
echo "API Level: $(adb -s "$EMULATOR_SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')"
echo "Device: $(adb -s "$EMULATOR_SERIAL" shell getprop ro.product.model | tr -d '\r')"

print_header "Ready to Run Tests!"

echo "The emulator is now running and ready for UI tests."
echo ""
echo "To run the UI tests, execute:"
echo "  cd ui-tests"
echo "  ./run-ui-tests.sh"
echo ""
echo "To stop the emulator:"
echo "  adb -s $EMULATOR_SERIAL emu kill"
echo ""

