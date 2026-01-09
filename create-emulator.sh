#!/bin/bash

set -e

echo "🚀 Creating Android Virtual Device..."

# Set environment
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"

# Create AVD
echo "📱 Creating AVD 'Ubuntu_Test'..."
avdmanager create avd \
  --name Ubuntu_Test \
  --package "system-images;android-34;google_apis;arm64-v8a" \
  --device "pixel_6_pro" \
  --force

echo "✅ AVD created successfully!"
echo ""
echo "To launch the emulator, run:"
echo "  ./launch-emulator.sh"
