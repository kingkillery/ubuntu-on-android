#!/bin/bash

# Install debug APK to connected Android device

set -e

echo "Installing Ubuntu on Android debug APK..."

# Build debug APK
./gradlew assembleDebug

# Install to device
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo "✓ Installation complete!"
