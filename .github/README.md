# GitHub Actions Workflows

This directory contains CI/CD workflows for the Czech Declension Quiz project.

## Workflows

### 1. Build APK (`build.yml`)

**Purpose**: Fast feedback on build status

- **Triggers**: Push/PR to `main` or `develop` branches
- **Runner**: `ubuntu-latest` (fast, cheap)
- **Duration**: ~5 minutes
- **Steps**:
  1. Checkout code
  2. Set up JDK 21
  3. Set up Android SDK
  4. Build debug APK
  5. Build release APK
  6. Upload APKs as artifacts

**Artifacts**:
- `app-debug.apk` (7 days retention)
- `app-release-unsigned.apk` (30 days retention)

---

### 2. UI Tests (`ui-tests.yml`)

**Purpose**: Automated UI testing on Android emulator

- **Triggers**: Push/PR to `main` or `develop` branches, manual dispatch
- **Runner**: `macos-latest` (required for Android emulator)
- **Duration**: ~20-30 minutes
- **Steps**:
  1. Checkout code
  2. Set up JDK 21 and Node.js
  3. Install Appium using `setup-appium` action
  4. Set up Android SDK
  5. Create Android Virtual Device (AVD)
  6. Build test database from JSON
  7. Build release APK
  8. Start Android emulator with Appium
  9. Run UI tests
  10. Upload test reports and screenshots

**Artifacts**:
- Test reports (HTML and XML)
- Test results (JUnit format)
- Screenshots (on failure)
- Appium logs

**Features**:
- ✅ Publishes test results as GitHub check
- ✅ Comments on PRs with test status
- ✅ Captures screenshots on failures
- ✅ Uploads detailed logs for debugging

---

## GitHub Actions Used

### Official Actions
- **actions/checkout@v4** - Checkout repository code
- **actions/setup-java@v4** - Set up JDK 21 (Temurin distribution)
- **actions/setup-node@v4** - Set up Node.js for Appium
- **actions/upload-artifact@v4** - Upload build artifacts and test reports
- **android-actions/setup-android@v3** - Set up Android SDK

### Third-Party Actions
- **youngfreeFJS/setup-appium@v0.1.6** - Install and configure Appium server
- **reactivecircus/android-emulator-runner@v2** - Run Android emulator on macOS
- **EnricoMi/publish-unit-test-result-action@v2** - Publish test results
- **actions/github-script@v7** - Comment on PRs with test status

---

## Configuration

### Environment Variables

Both workflows use:
- `ANDROID_HOME`: Set automatically by `setup-android` action
- `ANDROID_SDK_ROOT`: Set to same as `ANDROID_HOME`

### Caching

Workflows cache:
- Gradle dependencies (via `setup-java`)
- npm packages (via `setup-node`)
- Android SDK components

This speeds up subsequent runs significantly.

### Timeouts

- Build workflow: 15 minutes
- UI tests workflow: 30 minutes

These prevent stuck jobs from consuming runner minutes.

---

## Manual Workflow Dispatch

The UI tests workflow can be triggered manually:

1. Go to **Actions** tab in GitHub
2. Select **UI Tests** workflow
3. Click **Run workflow**
4. Select branch
5. Click **Run workflow** button

---

## Viewing Results

### Build Workflow

- Check the **Actions** tab for build status
- Download APKs from **Artifacts** section
- View build summary in workflow run

### UI Tests Workflow

- Check the **Actions** tab for test status
- View test results in the **Checks** tab (on PRs)
- Download test reports from **Artifacts**:
  - `test-reports/` - HTML reports
  - `appium-logs/` - Appium server logs
- View PR comments for quick status

---

## Troubleshooting

### Build fails with "Java version" error

The project requires JDK 21. The workflow is configured to use Temurin 21.

### UI tests fail with "Emulator timeout"

The emulator may take longer to start on GitHub runners. The workflow waits up to 30 seconds for Appium to be ready.

### Tests pass locally but fail in CI

Check:
- Appium logs in artifacts
- Screenshots in artifacts
- Test reports for detailed error messages

### Workflow doesn't trigger

Check:
- Branch name matches trigger configuration
- Workflow file is in `.github/workflows/`
- YAML syntax is valid

---

## Cost Optimization

### Build Workflow
- Uses Ubuntu runner (cheapest)
- Caches dependencies
- Runs in ~5 minutes
- **Estimated cost**: ~$0.01 per run

### UI Tests Workflow
- Uses macOS runner (more expensive, but required)
- Only runs on important branches
- Can be triggered manually for testing
- Caches dependencies to reduce runtime
- **Estimated cost**: ~$0.50 per run

**Tip**: Use the build workflow for quick feedback, and UI tests for final validation before merging.

