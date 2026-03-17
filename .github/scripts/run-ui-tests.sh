#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOG_DIR="$ROOT_DIR/ui-tests/build/test-results/logs"
APPIUM_LOG_FILE="$LOG_DIR/appium.log"
LOGCAT_FULL_FILE="$LOG_DIR/logcat-full.log"
LOGCAT_APP_FILE="$LOG_DIR/logcat-com-usharik-app.log"
LOG_INDEX_JSON="$LOG_DIR/index.json"
LOG_SUMMARY_MD="$LOG_DIR/summary.md"

mkdir -p "$LOG_DIR"

file_size_bytes() {
  local file_path="$1"
  if [ -f "$file_path" ]; then
    wc -c < "$file_path" | tr -d '[:space:]'
  else
    echo "0"
  fi
}

cleanup() {
  local exit_code=$?

  # Always collect device logs from this test run.
  adb logcat -d -v threadtime > "$LOGCAT_FULL_FILE" 2>&1 || true
  grep -E "com\.usharik\.app|UiTests" "$LOGCAT_FULL_FILE" > "$LOGCAT_APP_FILE" 2>/dev/null || true

  cat > "$LOG_INDEX_JSON" <<EOF
{
  "artifact": "ui-test-logs",
  "generatedAtUtc": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "paths": {
    "appium": "ui-tests/build/test-results/logs/appium.log",
    "logcatFull": "ui-tests/build/test-results/logs/logcat-full.log",
    "logcatApp": "ui-tests/build/test-results/logs/logcat-com-usharik-app.log",
    "summary": "ui-tests/build/test-results/logs/summary.md"
  },
  "sizesBytes": {
    "appium": $(file_size_bytes "$APPIUM_LOG_FILE"),
    "logcatFull": $(file_size_bytes "$LOGCAT_FULL_FILE"),
    "logcatApp": $(file_size_bytes "$LOGCAT_APP_FILE")
  }
}
EOF

  cat > "$LOG_SUMMARY_MD" <<'EOF'
## UI test debug logs

- Artifact name: `ui-test-logs`
- Appium log: `ui-tests/build/test-results/logs/appium.log`
- Full logcat: `ui-tests/build/test-results/logs/logcat-full.log`
- Filtered app logcat: `ui-tests/build/test-results/logs/logcat-com-usharik-app.log`
- Machine index: `ui-tests/build/test-results/logs/index.json`

### How to inspect logs

1. Open the `ui-test-logs` artifact in this workflow run.
2. Start with `summary.md` or `index.json` to see the available files.
3. Check `logcat-com-usharik-app.log` first for app-specific failures.
4. Use `logcat-full.log` for emulator / Android framework issues.
5. Use `appium.log` for driver, session, or locator/debugging problems.
EOF

  if [ -n "${APPIUM_PID:-}" ]; then
    kill "$APPIUM_PID" >/dev/null 2>&1 || true
  fi

  exit "$exit_code"
}

trap cleanup EXIT

# Start Appium in background
export ANDROID_HOME=$ANDROID_HOME
export ANDROID_SDK_ROOT=$ANDROID_HOME
appium --log-timestamp --log-no-colors > "$APPIUM_LOG_FILE" 2>&1 &
APPIUM_PID=$!

# Clear old device logs so only this run is captured.
adb logcat -c || true

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
cd "$ROOT_DIR/ui-tests"
echo "Starting UI tests..."
if ./run-ui-tests.sh --skip-build; then
  echo "✅ Tests passed!"
  TEST_EXIT_CODE=0
else
  echo "❌ Tests failed!"
  TEST_EXIT_CODE=1
fi

# Exit with test result code (cleanup trap will gather logs and stop Appium)
exit "$TEST_EXIT_CODE"

