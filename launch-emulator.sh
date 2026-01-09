#!/bin/bash

echo "🚀 Launching Android Emulator..."

# Set environment
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"

# Launch emulator in background
echo "📱 Starting emulator 'Ubuntu_Test'..."
$ANDROID_HOME/emulator/emulator -avd Ubuntu_Test -gpu host > /dev/null 2>&1 &

EMULATOR_PID=$!
echo "✅ Emulator started (PID: $EMULATOR_PID)"
echo "⏳ Waiting for emulator to boot (this may take 30-60 seconds)..."

# Wait for device
adb wait-for-device
sleep 10  # Extra time for full boot

echo ""
echo "✅ Emulator is ready!"
echo "📱 You should see the Android home screen"
echo ""
echo "Now run: ./test-on-emulator.sh"
