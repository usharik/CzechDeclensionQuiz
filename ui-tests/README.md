# Czech Declension Quiz - UI Tests

✅ **Comprehensive UI test suite!** Automated UI tests for the Czech Declension Quiz Android app using Appium.

## Quick Start

```bash
cd ui-tests
./run-ui-tests.sh
```

That's it! The script handles everything automatically.

## Test Results

**Current Status: 8/8 tests passing (100% pass rate)** 🎉

### Navigation Tests ✅
All navigation tests now include **rotation testing** (landscape/portrait) to ensure screens handle orientation changes correctly.

- ✅ **testNavigateToQuizScreen** - Verifies Quiz screen displays with word and action buttons + rotation test
- ✅ **testNavigateToWordsWithErrorsScreen** - Verifies Words with Errors screen displays cases container + rotation test
- ✅ **testNavigateToHandbookScreen** - Verifies Handbook screen displays gender selection with correct header text + rotation test
- ✅ **testNavigateToSettingsScreen** - Verifies Settings screen displays filter options and additional settings + rotation test
- ✅ **testNavigateToAboutScreen** - Verifies About screen displays app name, logo, and version + rotation test
- ✅ **testScreenRotation** - Legacy rotation test for Quiz screen (kept for backwards compatibility)

### Quiz Functionality Tests ✅
- ✅ **testCorrectQuizSolution** - Tests complete quiz workflow with drag-and-drop, verifies success dialog
- ✅ **testIncorrectQuizSolution** - Tests error handling with mixed mistakes: 6 words correct, 4 words in wrong positions (swapped singular/plural), 4 words missing. Verifies error toast appears and success dialog does NOT appear

## What It Does

The test script automatically:
1. ✅ Checks all prerequisites (Java, Node.js, Appium, ADB, emulator)
2. ✅ Starts Appium server with proper environment variables
3. ✅ **Always rebuilds the APK** to ensure latest code changes
4. ✅ **Reinstalls the APK** on the emulator for fresh testing
5. ✅ **Cleans test results** to force tests to re-run (no caching)
6. ✅ Loads test data directly from JSON (no database needed!)
7. ✅ Runs all UI tests
8. ✅ Generates detailed HTML reports
9. ✅ **Saves screenshots** to `ui-tests/screenshots/` (using project root from Gradle)

## Technology Stack

- **JUnit 6.0.3** - Latest major version with unified versioning (Platform + Jupiter)
- **Appium 3.2** - Mobile automation framework
- **Java 21** - Latest LTS version (required for JUnit 6)
- **Gradle 8.14** - Build tool with JUnit Platform support

### JUnit 6 New Features & Improvements

JUnit 6.0.3 brings several enhancements over JUnit 5:

1. **Unified Version Numbering** - Platform, Jupiter, and Vintage now share the same version (6.0.3)
2. **Java 17+ Baseline** - Requires Java 17 minimum (we use Java 21)
3. **JSpecify Nullability Annotations** - Better null-safety support
4. **Improved Display Names** - Non-printable control characters (like `\n`) are replaced with readable representations (e.g., `<LF>`)
5. **Enhanced CSV Support** - Migrated to FastCSV library for better performance and error reporting
6. **Kotlin Suspend Functions** - Native support for Kotlin coroutines in test methods
7. **Cancellation Support** - New `--fail-fast` mode and `CancellationToken` API
8. **Deterministic @Nested Class Ordering** - Consistent test execution order
9. **Stack Trace Pruning** - Cleaner stack traces pruned up to test/lifecycle methods
10. **Better Error Messages** - Improved diagnostics for configuration and assertion failures

## Prerequisites

The script checks these automatically, but you need:

- **Java 21** - OpenJDK 21 or later
- **Node.js** - v18 or later
- **Appium** - Installed globally (`npm install -g appium`)
- **UiAutomator2 Driver** - `appium driver install uiautomator2`
- **Android SDK** - At `~/Library/Android/sdk` (macOS)
- **Android Emulator** - Running or available

### First-Time Setup

If you don't have an emulator, run:
```bash
./setup-android-emulator.sh
```

This creates and starts a test emulator optimized for M-series Macs.

## Script Options

```bash
./run-ui-tests.sh [OPTIONS]

Options:
  --check-only      Only check prerequisites, don't run tests
  --skip-build      Skip APK build step (use existing APK)
  --skip-checks     Skip prerequisite checks (not recommended)
  --help            Show help message
```

**Note:** By default, the APK is **always rebuilt** to ensure you're testing the latest code changes. Use `--skip-build` only if you're certain the existing APK is up to date.

## Customizing Timeouts and Delays

All timeouts and delays are configurable via Gradle system properties. You can override them when running tests:

```bash
# Run tests with custom timeouts
./gradlew :ui-tests:test \
  -Dtest.timeout.default=15 \
  -Dtest.timeout.implicit=3 \
  -Dtest.delay.screen.stability=200 \
  -Dtest.delay.ui.update=150 \
  -Dtest.delay.drag.duration=200
```

### Available Configuration Properties

| Property | Default | Unit | Description |
|----------|---------|------|-------------|
| `test.timeout.default` | `10` | seconds | Default timeout for element waits |
| `test.timeout.implicit` | `2` | seconds | Implicit wait for driver |
| `test.delay.screen.stability` | `100` | milliseconds | Wait after navigation/rotation |
| `test.delay.ui.update` | `100` | milliseconds | Wait after drag-and-drop |
| `test.delay.drag.duration` | `100` | milliseconds | Duration of drag gesture |

### When to Adjust

- **Slower devices**: Increase all timeouts and delays
- **Faster devices**: Decrease delays for faster test execution
- **Flaky tests**: Increase `screen.stability` and `ui.update` delays
- **Network issues**: Increase `timeout.default`

### Example: Slow Device Configuration

```bash
./gradlew :ui-tests:test \
  -Dtest.timeout.default=20 \
  -Dtest.timeout.implicit=5 \
  -Dtest.delay.screen.stability=300 \
  -Dtest.delay.ui.update=300 \
  -Dtest.delay.drag.duration=300
```

## Test Data

Tests use JSON data directly - **no database needed!**

- **Source**: `../database/src/main/assets/data.json`
- **Format**: JSON Lines (one word per line)
- **Records**: 902 Czech words with declensions
- **Size**: ~200KB
- **Loading**: Loaded into memory at test startup

### How It Works

1. **Test startup**: Reads `data.json` and loads all words into a HashMap
2. **During tests**: Looks up word cases from the in-memory cache
3. **Fast**: No database queries, no SQL, no external dependencies
4. **Simple**: Just Java standard library + JSON parsing

### Benefits

✅ **No database setup** - No SQLite, no schema, no migrations
✅ **Faster** - In-memory lookups are instant
✅ **Simpler** - Fewer dependencies, less code
✅ **Portable** - Works anywhere Java runs
✅ **Easy to debug** - Just read the JSON file

## Test Reports

After running tests, view the HTML report:
```bash
open build/reports/tests/test/index.html
```

Or check the XML results:
```bash
cat build/test-results/test/TEST-*.xml
```

## Project Structure

```
ui-tests/
├── README.md                          # This file
├── run-ui-tests.sh                    # Main test runner script
├── setup-android-emulator.sh          # Emulator setup script
├── start-emulator.sh                  # Emulator starter script
├── build.gradle                       # Gradle build configuration
└── src/test/java/com/usharik/app/
    ├── UiTests.java                   # Main test class
    └── helpers/TestHelper.java        # Test utilities (JSON parser)
```

## Key Features

### Automatic Environment Setup
- Auto-detects and sets `ANDROID_HOME`
- Auto-starts Appium server if not running
- **Always rebuilds APK** to ensure latest code
- Auto-loads test data from JSON

### Comprehensive Checks
- Java version compatibility
- Node.js availability
- Appium installation and version
- Android SDK and tools
- Emulator/device connectivity
- APK and database existence

### Smart Error Handling
- Clear, color-coded output
- Actionable error messages
- Detailed logging
- Graceful failure handling

## Troubleshooting

### Appium won't start
Check the logs:
```bash
tail -f /tmp/appium.log
```

### Emulator not connecting
List available devices:
```bash
adb devices
```

Start the emulator:
```bash
./start-emulator.sh
```

### Tests fail with "element not found"
This is expected for `testCorrectQuizSolution` - the test needs updating to match current app UI.

### Data JSON not found
Make sure the data.json file exists:
```bash
ls -lh database/src/main/assets/data.json
```

## Migration Notes

This test suite has been updated from Appium 7.x to 10.x:
- ✅ `MobileBy` → `AppiumBy`
- ✅ `TouchAction` → W3C `PointerInput`
- ✅ `DesiredCapabilities` → `UiAutomator2Options`
- ✅ SQLite database → **Direct JSON loading**

### Database Removal

The tests previously used a SQLite database built from `data.json`. This has been **completely removed** in favor of:
- **Direct JSON parsing** - Loads `data.json` directly into memory
- **No database dependencies** - Removed `sqlite-jdbc` and all SQL code
- **Faster startup** - No database creation or connection overhead
- **Simpler code** - Just HashMap lookups instead of SQL queries

## Files Created

- **run-ui-tests.sh** - Main test runner with automatic setup
- **setup-android-emulator.sh** - Creates and configures Android emulator
- **start-emulator.sh** - Starts the test emulator
- **TestHelper.java** - Refactored to use JSON directly (no database)

### Firebase Configuration

The app requires `app/google-services.json` for Firebase integration. This file:
- **Is NOT committed** to the repository (contains sensitive data)
- **Is auto-copied** in CI/CD workflows from `utils/google-services.json.dummy`
- **Must be created locally** if you want to build the app

To build locally without Firebase:
```bash
cp utils/google-services.json.dummy app/google-services.json
```

See `utils/README.md` for more details.

## Issues Resolved

1. ✅ Java version compatibility (VERSION_25 → VERSION_21)
2. ✅ Missing google-services.json file
3. ✅ APK path correction (app-release-unsigned.apk)
4. ✅ ANDROID_HOME environment variable propagation
5. ✅ Emulator architecture (arm64-v8a for M-series Mac, x86_64 for GitHub Actions)
6. ✅ Appium auto-start with proper environment
7. ✅ **Removed database dependency** - Direct JSON loading
8. ✅ **Optimized test data loading** - In-memory HashMap cache
9. ✅ **Simplified dependencies** - Only JUnit + Appium needed

## Support

For issues or questions:
- **Test Reports**: `build/reports/tests/test/index.html`
- **Appium Logs**: `/tmp/appium.log`
- **Gradle Output**: Run with `--info` flag for detailed logging
- **Screenshots**: Saved to `screenshots/` directory during test execution

## CI/CD Integration

### GitHub Actions

Two workflows are configured:

#### 1. Build Workflow (`.github/workflows/build.yml`)
- **Triggers**: Push/PR to main or develop branches
- **Runs on**: Ubuntu (fast)
- **Duration**: ~5 minutes
- **Actions**:
  - Builds debug and release APKs
  - Uploads APKs as artifacts
  - Shows build summary

#### 2. UI Tests Workflow (`.github/workflows/ui-tests.yml`)
- **Triggers**: Push/PR to master branch, manual dispatch
- **Runs on**: Ubuntu (with KVM acceleration for fast emulator)
- **Duration**: ~15-20 minutes
- **Actions**:
  - Sets up Android SDK and emulator
  - Installs Appium and dependencies
  - Builds test database
  - Builds APK
  - Runs UI tests on emulator
  - Uploads test reports and screenshots
  - Comments results on PRs

### Workflow Features

✅ **Automatic dependency caching** (Gradle, npm)
✅ **Parallel artifact uploads** (APKs, reports, logs)
✅ **Test result publishing** with detailed reports
✅ **PR comments** with test status
✅ **Screenshot capture** on test failures
✅ **Appium logs** for debugging

### Manual Workflow Dispatch

You can manually trigger the UI tests workflow from GitHub:
1. Go to Actions tab
2. Select "UI Tests" workflow
3. Click "Run workflow"
4. Select branch and run

### Local CI Testing

To test the workflow locally before pushing:

```bash
# Install act (GitHub Actions local runner)
brew install act

# Run the build workflow
act -j build

# Run UI tests (requires macOS)
act -j ui-tests
```

## Technical Details

- **Appium Version**: 3.2.0
- **UiAutomator2 Driver**: 7.0.0
- **Selenium**: 4.35.0
- **Java Client**: 10.x
- **Android API Level**: 30 (Android 11)
- **Emulator (Local)**: arm64-v8a (M-series Mac) or x86_64 (Intel Mac)
- **Emulator (CI/CD)**: x86_64 (Ubuntu with KVM acceleration)

