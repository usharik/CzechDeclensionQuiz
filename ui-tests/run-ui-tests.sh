#!/bin/bash

# UI Tests Runner Script for Czech Declension Quiz
# This script handles all test running logic including prerequisites,
# building APK, and executing tests

set -e

# Script directory and project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Default paths
DEFAULT_APK_PATH="$PROJECT_ROOT/app/build/outputs/apk/release/app-release-unsigned.apk"
DEFAULT_DATA_JSON_PATH="$PROJECT_ROOT/database/src/main/assets/data.json"

# Allow overriding via environment variables or arguments
APK_PATH="${APK_PATH:-$DEFAULT_APK_PATH}"
DATA_JSON_PATH="${DATA_JSON_PATH:-$DEFAULT_DATA_JSON_PATH}"

# Parse command line arguments
SKIP_BUILD=false
SKIP_CHECKS=false
CHECK_ONLY=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --skip-checks)
            SKIP_CHECKS=true
            shift
            ;;
        --check-only)
            CHECK_ONLY=true
            shift
            ;;
        --apk-path)
            APK_PATH="$2"
            shift 2
            ;;
        --data-json-path)
            DATA_JSON_PATH="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --skip-build         Skip building the APK"
            echo "  --skip-checks        Skip prerequisite checks"
            echo "  --check-only         Only check prerequisites, don't run tests"
            echo "  --apk-path PATH      Custom APK path"
            echo "  --data-json-path PATH Custom data.json path"
            echo "  --help               Show this help message"
            echo ""
            echo "Environment variables:"
            echo "  APK_PATH             Override default APK path"
            echo "  DATA_JSON_PATH       Override default data.json path"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

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

# Function to check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"

    local all_good=true

    # Check Java
    if command_exists java; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1)
        print_status "success" "Java is installed: $JAVA_VERSION"
    else
        print_status "error" "Java is not installed"
        all_good=false
    fi

    # Check Node.js
    if command_exists node; then
        NODE_VERSION=$(node --version)
        print_status "success" "Node.js is installed: $NODE_VERSION"
    else
        print_status "error" "Node.js is not installed"
        echo "  Install from: https://nodejs.org/"
        all_good=false
    fi

    # Check Appium
    if command_exists appium; then
        APPIUM_VERSION=$(appium --version)
        print_status "success" "Appium is installed: v$APPIUM_VERSION"
    else
        print_status "error" "Appium is not installed"
        echo "  Install with: npm install -g appium"
        echo "  Then install driver: appium driver install uiautomator2"
        all_good=false
    fi

    # Check if Appium server is running
    if curl -s --max-time 5 http://localhost:4723/status > /dev/null 2>&1; then
        print_status "success" "Appium server is running on port 4723"
    else
        print_status "warning" "Appium server is not running, starting it..."
        # Start Appium with ANDROID_HOME set
        if [ -z "$ANDROID_HOME" ]; then
            export ANDROID_HOME="$HOME/Library/Android/sdk"
        fi
        export ANDROID_SDK_ROOT="$ANDROID_HOME"

        nohup appium --log-timestamp --log-no-colors > /tmp/appium.log 2>&1 &
        sleep 3

        # Check if it started successfully
        if curl -s --max-time 5 http://localhost:4723/status > /dev/null 2>&1; then
            print_status "success" "Appium server started successfully"
        else
            print_status "error" "Failed to start Appium server"
            echo "  Check logs at: /tmp/appium.log"
            all_good=false
        fi
    fi

    # Check ADB
    # Try to find ADB in common locations if not in PATH
    if ! command_exists adb; then
        if [ -f "$HOME/Library/Android/sdk/platform-tools/adb" ]; then
            export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"
            print_status "info" "Added Android SDK platform-tools to PATH"
        fi
    fi

    if command_exists adb; then
        print_status "success" "ADB is installed"

        # Check for connected devices
        DEVICES=$(adb devices | grep -v "List" | grep "device" | wc -l | tr -d ' ')
        if [ "$DEVICES" -gt 0 ]; then
            print_status "success" "Android device/emulator is connected ($DEVICES device(s))"
            # Show device details
            echo ""
            echo "  Connected devices:"
            adb devices | grep "device$" | sed 's/^/    /'
        else
            print_status "error" "No Android device/emulator connected"
            echo "  Connect a device or start an emulator"
            echo "  Check with: adb devices"
            all_good=false
        fi
    else
        print_status "error" "ADB is not installed"
        echo "  Install Android SDK Platform Tools"
        all_good=false
    fi

    echo ""

    # Check APK
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        print_status "success" "APK found: $APK_PATH ($APK_SIZE)"
    else
        print_status "warning" "APK not found: $APK_PATH"
        if [ "$SKIP_BUILD" = true ]; then
            echo "  Cannot proceed without APK"
            all_good=false
        else
            echo "  Will build APK before running tests"
        fi
    fi

    # Check data.json file
    if [ -f "$DATA_JSON_PATH" ]; then
        JSON_SIZE=$(du -h "$DATA_JSON_PATH" | cut -f1)
        JSON_LINES=$(wc -l < "$DATA_JSON_PATH" | tr -d ' ')
        print_status "success" "Data JSON found: $DATA_JSON_PATH ($JSON_SIZE, $JSON_LINES words)"
    else
        print_status "error" "Data JSON not found: $DATA_JSON_PATH"
        echo "  This file is required for tests"
        all_good=false
    fi

    echo ""

    if [ "$all_good" = false ]; then
        print_status "error" "Some prerequisites are not met"
        return 1
    else
        print_status "success" "All prerequisites met!"
        return 0
    fi
}


# Function to build APK
build_apk() {
    print_header "Building APK"

    cd "$PROJECT_ROOT"

    print_status "info" "Running: ./gradlew :app:assembleRelease"
    echo ""

    if ./gradlew :app:assembleRelease; then
        print_status "success" "APK built successfully"

        if [ -f "$APK_PATH" ]; then
            APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
            print_status "info" "APK location: $APK_PATH ($APK_SIZE)"
        fi
        return 0
    else
        print_status "error" "Failed to build APK"
        return 1
    fi
}

# Function to run tests
run_tests() {
    print_header "Running UI Tests"

    cd "$PROJECT_ROOT"

    # Ensure ANDROID_HOME is set
    if [ -z "$ANDROID_HOME" ]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
    fi
    export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

    print_status "info" "Test configuration:"
    echo "  APK Path: $APK_PATH"
    echo "  Data JSON Path: $DATA_JSON_PATH"
    echo "  ANDROID_HOME: $ANDROID_HOME"
    echo ""

    print_status "info" "Running: ./gradlew :ui-tests:test"
    echo ""

    # Run tests with system properties and environment variables
    if ANDROID_HOME="$ANDROID_HOME" ./gradlew :ui-tests:test \
        -Dapp.path="$APK_PATH" \
        -Ddata.json.path="$DATA_JSON_PATH" \
        --info; then
        print_status "success" "Tests completed successfully"
        return 0
    else
        print_status "error" "Tests failed"
        return 1
    fi
}

# Main execution
main() {
    print_header "Czech Declension Quiz - UI Tests Runner"

    print_status "info" "Project root: $PROJECT_ROOT"

    # Check prerequisites unless skipped
    if [ "$SKIP_CHECKS" = false ]; then
        if ! check_prerequisites; then
            echo ""
            print_status "error" "Prerequisites check failed. Fix the issues above and try again."
            echo ""
            echo "To skip checks (not recommended), use: $0 --skip-checks"
            exit 1
        fi
    else
        print_status "warning" "Skipping prerequisite checks"
    fi

    # Exit if check-only mode
    if [ "$CHECK_ONLY" = true ]; then
        echo ""
        print_status "info" "Check-only mode. Exiting."
        exit 0
    fi

    # Build APK if needed
    if [ "$SKIP_BUILD" = false ]; then
        if [ ! -f "$APK_PATH" ]; then
            print_status "info" "APK not found, building..."
            if ! build_apk; then
                echo ""
                print_status "error" "Failed to build APK. Cannot proceed with tests."
                exit 1
            fi
        else
            print_status "info" "APK already exists, skipping build"
            echo "  Use --skip-build to always skip, or delete APK to force rebuild"
        fi
    else
        print_status "warning" "Skipping APK build"
        if [ ! -f "$APK_PATH" ]; then
            print_status "error" "APK not found and build skipped. Cannot proceed."
            exit 1
        fi
    fi

    # Run tests
    if ! run_tests; then
        echo ""
        print_status "error" "Test execution failed"
        exit 1
    fi

    # Success
    print_header "✓ All Tests Completed Successfully!"

    echo "Test artifacts:"
    echo "  Screenshots: $PROJECT_ROOT/ui-tests/screenshots/"
    echo "  Test reports: $PROJECT_ROOT/ui-tests/build/reports/tests/test/index.html"
    echo ""
}

# Run main function
main

