#!/bin/bash
set -e

# Start Appium in background
export ANDROID_HOME=$ANDROID_HOME
export ANDROID_SDK_ROOT=$ANDROID_HOME
appium --log-timestamp --log-no-colors > /tmp/appium.log 2>&1 &
APPIUM_PID=$!

# Wait for Appium to start
echo "Waiting for Appium to start..."
for i in {1..60}; do
  if curl -s http://localhost:4723/status > /dev/null 2>&1; then
    echo "Appium is ready!"
    break
  fi
  sleep 1
done

# Wait for emulator to be ready
adb wait-for-device
adb shell input keyevent 82
sleep 5

# Run tests
cd ui-tests
echo "Starting UI tests..."
if ./run-ui-tests.sh --skip-build; then
  echo "✅ Tests passed!"
  TEST_EXIT_CODE=0
else
  echo "❌ Tests failed!"
  TEST_EXIT_CODE=1
fi

# Stop Appium
kill $APPIUM_PID || true

# Exit with test result code
exit $TEST_EXIT_CODE

