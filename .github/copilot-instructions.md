# Copilot instructions for UI test reports

When analysing UI test failures in this repository, use the GitHub Actions artifacts and summary in the following order:

## Primary sources

1. Check the workflow run summary first.
   - The `UI Tests` workflow publishes a short log summary into the run summary.
   - It also contains direct links to the `test-reports` and `ui-test-logs` artifacts.

2. Use the `test-reports` artifact as the main source of truth for test outcome.
   - Main result file: `ui-tests/build/test-results/test/TEST-com.usharik.app.UiTests.xml`
   - Also inspect HTML/JUnit reports under `ui-tests/build/reports/tests/` and screenshots under `ui-tests/screenshots/`.

3. Use the `ui-test-logs` artifact only for diagnosis and root-cause analysis.
   - Start with `ui-tests/build/test-results/logs/summary.md`
   - Then inspect `ui-tests/build/test-results/logs/index.json` if a machine-readable file list is useful
   - Then inspect logs in this order:
     1. `ui-tests/build/test-results/logs/logcat-com-usharik-app.log`
     2. `ui-tests/build/test-results/logs/logcat-full.log`
     3. `ui-tests/build/test-results/logs/appium.log`

## Expected handling flow

- First summarize whether tests passed, failed, or did not execute.
- Use the JUnit XML report to identify failed tests and counts.
- Only after that, use `ui-test-logs` to explain likely root cause.
- Prefer app-specific logcat before full logcat to reduce noise.
- Use Appium logs mainly for session creation, locator, and driver issues.

## Notes

- The workflow file is `.github/workflows/ui-tests.yml`.
- The log collection script is `.github/scripts/run-ui-tests.sh`.
- If logs are missing, say that explicitly and fall back to workflow console output plus available test reports.

