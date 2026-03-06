#!/bin/bash

# Android Emulator Setup Script
# This script downloads Android SDK tools, creates an emulator, and starts it

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
CMDLINE_TOOLS_VERSION="11076708"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-mac-${CMDLINE_TOOLS_VERSION}_latest.zip"
EMULATOR_NAME="test_emulator"
DEVICE_TYPE="pixel_4"

# Detect architecture and set appropriate system image
ARCH=$(uname -m)
if [ "$ARCH" = "arm64" ]; then
    # Apple Silicon Mac - use ARM64 system image
    SYSTEM_IMAGE="system-images;android-30;google_apis;arm64-v8a"
else
    # Intel Mac - use x86_64 system image
    SYSTEM_IMAGE="system-images;android-30;google_apis;x86_64"
fi

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

# Check if running on macOS
if [[ "$OSTYPE" != "darwin"* ]]; then
    print_status "error" "This script is designed for macOS"
    exit 1
fi

print_header "Android Emulator Setup"

# Show architecture info
if [ "$ARCH" = "arm64" ]; then
    print_status "info" "Detected Apple Silicon Mac - using ARM64 system image"
else
    print_status "info" "Detected Intel Mac - using x86_64 system image"
fi

# Create Android SDK directory
print_status "info" "Setting up Android SDK at: $ANDROID_HOME"
mkdir -p "$ANDROID_HOME"

# Download command-line tools if not present
if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
    print_header "Downloading Android Command Line Tools"
    
    TEMP_DIR=$(mktemp -d)
    cd "$TEMP_DIR"
    
    print_status "info" "Downloading from: $CMDLINE_TOOLS_URL"
    curl -o cmdline-tools.zip "$CMDLINE_TOOLS_URL"
    
    print_status "info" "Extracting..."
    unzip -q cmdline-tools.zip
    
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    mv cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
    
    cd - > /dev/null
    rm -rf "$TEMP_DIR"
    
    print_status "success" "Command line tools installed"
else
    print_status "success" "Command line tools already installed"
fi

# Set up environment
export ANDROID_HOME
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

# Accept licenses
print_header "Accepting Android SDK Licenses"
yes | sdkmanager --licenses > /dev/null 2>&1 || true
print_status "success" "Licenses accepted"

# Install required SDK components
print_header "Installing SDK Components"

print_status "info" "Installing platform-tools..."
sdkmanager "platform-tools" > /dev/null 2>&1

print_status "info" "Installing emulator..."
sdkmanager "emulator" > /dev/null 2>&1

print_status "info" "Installing system image (Android 11 / API 30)..."
sdkmanager "$SYSTEM_IMAGE" > /dev/null 2>&1

print_status "success" "SDK components installed"

# Check if emulator already exists
if avdmanager list avd | grep -q "Name: $EMULATOR_NAME"; then
    print_status "warning" "Emulator '$EMULATOR_NAME' already exists"
    read -p "Do you want to delete and recreate it? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        avdmanager delete avd -n "$EMULATOR_NAME"
        print_status "info" "Deleted existing emulator"
    else
        print_status "info" "Using existing emulator"
        SKIP_CREATE=true
    fi
fi

# Create emulator
if [ "$SKIP_CREATE" != "true" ]; then
    print_header "Creating Android Emulator"
    
    print_status "info" "Creating emulator: $EMULATOR_NAME"
    print_status "info" "Device: $DEVICE_TYPE"
    print_status "info" "System image: $SYSTEM_IMAGE"
    
    echo "no" | avdmanager create avd \
        -n "$EMULATOR_NAME" \
        -k "$SYSTEM_IMAGE" \
        -d "$DEVICE_TYPE" \
        --force
    
    print_status "success" "Emulator created"
fi

# Configure emulator for better performance
print_header "Configuring Emulator"

AVD_CONFIG="$HOME/.android/avd/${EMULATOR_NAME}.avd/config.ini"
if [ -f "$AVD_CONFIG" ]; then
    # Enable hardware acceleration
    if ! grep -q "hw.gpu.enabled" "$AVD_CONFIG"; then
        echo "hw.gpu.enabled=yes" >> "$AVD_CONFIG"
    fi
    if ! grep -q "hw.gpu.mode" "$AVD_CONFIG"; then
        echo "hw.gpu.mode=auto" >> "$AVD_CONFIG"
    fi
    print_status "success" "Emulator configured for better performance"
fi

print_header "Setup Complete!"

print_status "success" "Android SDK installed at: $ANDROID_HOME"
print_status "success" "Emulator '$EMULATOR_NAME' is ready"

echo ""
echo "To use the emulator, add these to your ~/.zshrc or ~/.bash_profile:"
echo ""
echo "export ANDROID_HOME=\"$ANDROID_HOME\""
echo "export PATH=\"\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/emulator:\$PATH\""
echo ""

