#!/bin/bash
set -e

echo "=== UI Test Runner Starting ==="

BOOT_COMPLETE_TIMEOUT=120

# Kill any existing Appium processes so the Java test can start its own cleanly
echo "Killing any existing Appium processes on port 4723..."
pkill -f "appium" || true
sleep 2

# Ensure port 4723 is free
if lsof -i :4723 > /dev/null 2>&1; then
  echo "Port 4723 still in use, forcefully freeing it..."
  lsof -ti :4723 | xargs kill -9 || true
  sleep 2
fi

echo "Port 4723 is free."

# Set up environment for Android
export ANDROID_SDK_ROOT=$ANDROID_HOME

# Wait for emulator to be fully ready
echo "Waiting for emulator to be ready..."
adb wait-for-device

# Wait for boot to complete (up to 120 seconds)
echo "Waiting for Android boot to complete..."
BOOT_COMPLETE=false
for i in $(seq 1 $BOOT_COMPLETE_TIMEOUT); do
  BOOT_STATUS=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
  if [ "$BOOT_STATUS" = "1" ]; then
    echo "Android boot completed after ${i}s!"
    BOOT_COMPLETE=true
    break
  fi
  sleep 1
done

if [ "$BOOT_COMPLETE" = false ]; then
  echo "⚠️ Boot completion check timed out, continuing anyway..."
fi

# Dismiss lock screen and ensure ADB is responsive
adb shell input keyevent 82 || true
sleep 3

# Log device state for debugging
echo "=== Device state ==="
adb devices -l
echo "=== ADB shell properties ==="
adb shell getprop ro.product.model || true

# Run tests
# --skip-checks: skip prerequisite check that would also try to start Appium
# --skip-build:  APK was already built in a prior workflow step
cd ui-tests
echo "=== Starting UI tests ==="
if ./run-ui-tests.sh --skip-checks --skip-build; then
  echo "✅ Tests passed!"
  TEST_EXIT_CODE=0
else
  echo "❌ Tests failed!"
  TEST_EXIT_CODE=1
fi

echo "=== UI Test Runner Finished (exit code: $TEST_EXIT_CODE) ==="

# Exit with test result code
exit $TEST_EXIT_CODE

