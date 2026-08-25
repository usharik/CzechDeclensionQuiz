# Czech Declension Quiz - UI Tests

Appium **smoke tests** for the Czech Declension Quiz Android app. Functional coverage (drag-and-drop, dialogs, counters, per-screen behavior) lives in the Compose instrumentation tests (`app/src/androidTest`, run with `./gradlew :app:connectedDebugAndroidTest`). This suite only verifies that the built APK installs, launches, and the main navigation flows work end to end through the real UiAutomator2 accessibility surface.

The tests locate Compose elements by their `testTag` values (see `TestTags` in `CzechQuizApp.kt`), which the app exposes as `resource-id`s via `testTagsAsResourceId`.

## Configure Appium

First install appium and uiautomator2 driver with npm:
```bash
npm install -g appium@2
appium driver install uiautomator2@2с
```

In some cases it's required to kill it before running tests
```bash
lsof -nP -iTCP:4723 -sTCP:LISTEN
kill -9 <PID>
```

## Quick Start

```bash
cd ui-tests
./run-ui-tests.sh
```

That's it! The script handles everything automatically.

## Test Cases

- **testHubScreenShowsAllNavigationButtons** - Hub shows all six navigation buttons with correct labels
- **testNavigateToPagesAndBack** - Opens Words with errors, Handbook, Settings and About; verifies the app bar title and returns to the hub
- **testFullDeclensionQuizOpensAndQuitsViaDialog** - Full-table quiz shows a word and the word bank; back opens the quit dialog and "Leave quiz" returns to the hub
- **testSingleCaseQuizAnswerFlow** - Single-case quiz answer locks the answers and unlocks "Next case"; advancing resets the state; quit via the dialog

## What It Does

The test script automatically:
1. ✅ Checks all prerequisites (Java, Node.js, Appium, ADB, emulator)
2. ✅ Starts Appium server with proper environment variables
3. ✅ **Always rebuilds the APK** to ensure latest code changes
4. ✅ **Reinstalls the APK** on the emulator for fresh testing
5. ✅ **Cleans test results** to force tests to re-run (no caching)
6. ✅ Runs all UI tests
7. ✅ Generates detailed HTML reports

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
  -Dtest.delay.ui.update=150
```

### Available Configuration Properties

| Property | Default | Unit | Description |
|----------|---------|------|-------------|
| `test.timeout.default` | `10` | seconds | Default timeout for element waits |
| `test.timeout.implicit` | `2` | seconds | Implicit wait for driver |
| `test.delay.screen.stability` | `100` | milliseconds | Wait after navigation |
| `test.delay.ui.update` | `100` | milliseconds | Wait after UI interaction |

### When to Adjust

- **Slower devices**: Increase all timeouts and delays
- **Faster devices**: Decrease delays for faster test execution
- **Flaky tests**: Increase `screen.stability` and `ui.update` delays
- **Network issues**: Increase `timeout.default`

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
    ├── UiTests.java                   # Smoke test class
    ├── UiConstants.java               # Compose testTag values used as locators
    └── Parameters.java                # Timeouts/paths from system properties
```

## Key Features

### Automatic Environment Setup
- Auto-detects and sets `ANDROID_HOME`
- Auto-starts Appium server if not running
- **Always rebuilds APK** to ensure latest code

### Comprehensive Checks
- Java version compatibility
- Node.js availability
- Appium installation and version
- Android SDK and tools
- Emulator/device connectivity
- APK existence

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
Element locators are Compose `testTag` values exposed as resource-ids. If a tag was renamed in the app, update `UiConstants.java` to match `TestTags` in `CzechQuizApp.kt`.

## Migration Notes

After the Jetpack Compose migration, this suite was reduced to a smoke test set:
- ✅ View-id locators (`com.usharik.app:id/...`) → Compose `testTag` resource-ids
- ✅ Drag-and-drop, dialog and counter coverage moved to `app/src/androidTest` instrumentation tests
- ✅ `data.jsonl` loading, `TestHelper`/`WordInfo` helpers and the `gson` dependency removed

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

## Support

For issues or questions:
- **Test Reports**: `build/reports/tests/test/index.html`
- **Appium Logs**: `/tmp/appium.log`
- **Gradle Output**: Run with `--info` flag for detailed logging

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

