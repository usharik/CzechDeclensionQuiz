#!/bin/bash

# Master Script - Setup Everything and Run UI Tests
# This script:
# 1. Sets up Android emulator (if needed)
# 2. Starts the emulator
# 3. Runs the UI tests

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

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

print_header "Complete UI Test Setup and Execution"

print_status "info" "This script will:"
echo "  1. Set up Android SDK and emulator (if needed)"
echo "  2. Start the Android emulator"
echo "  3. Run the UI tests"
echo ""

# Check if setup is needed
ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
NEEDS_SETUP=false

if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
    print_status "warning" "Android SDK not found"
    NEEDS_SETUP=true
fi

if [ ! -d "$ANDROID_HOME/emulator" ]; then
    print_status "warning" "Android emulator not found"
    NEEDS_SETUP=true
fi

# Run setup if needed
if [ "$NEEDS_SETUP" = true ]; then
    print_header "Step 1: Setting up Android SDK and Emulator"
    
    if [ ! -f "$SCRIPT_DIR/setup-android-emulator.sh" ]; then
        print_status "error" "setup-android-emulator.sh not found"
        exit 1
    fi
    
    chmod +x "$SCRIPT_DIR/setup-android-emulator.sh"
    "$SCRIPT_DIR/setup-android-emulator.sh"
    
    print_status "success" "Setup complete"
else
    print_status "success" "Android SDK and emulator already set up"
fi

# Start emulator
print_header "Step 2: Starting Android Emulator"

if [ ! -f "$SCRIPT_DIR/start-emulator.sh" ]; then
    print_status "error" "start-emulator.sh not found"
    exit 1
fi

chmod +x "$SCRIPT_DIR/start-emulator.sh"
"$SCRIPT_DIR/start-emulator.sh"

print_status "success" "Emulator is running"

# Run tests
print_header "Step 3: Running UI Tests"

if [ ! -f "$SCRIPT_DIR/run-ui-tests.sh" ]; then
    print_status "error" "run-ui-tests.sh not found"
    exit 1
fi

chmod +x "$SCRIPT_DIR/run-ui-tests.sh"
"$SCRIPT_DIR/run-ui-tests.sh"

# Done
print_header "✓ Complete!"

print_status "success" "All steps completed successfully"
echo ""
echo "Test results available at:"
echo "  - Screenshots: ui-tests/screenshots/"
echo "  - Test report: ui-tests/build/reports/tests/test/index.html"
echo ""
echo "To stop the emulator:"
echo "  adb emu kill"
echo ""

