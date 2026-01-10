#!/bin/bash

set -e

echo "🚀 Starting Android Emulator Test..."

# Check if emulator is running
if ! adb devices | grep -q "emulator"; then
    echo "⚠️  No emulator detected. Please start an emulator first."
    echo "   Run: open -a 'Android Studio' and launch an AVD"
    exit 1
fi

echo "✅ Emulator detected"

# Wait for device to be ready
echo "⏳ Waiting for device to be ready..."
adb wait-for-device
sleep 5  # Give it a few more seconds to fully boot

# Install APK
echo "📦 Installing APK..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
echo "🎯 Launching Ubuntu on Android..."
adb shell am start -n com.udroid.app.debug/.MainActivity

echo ""
echo "✅ App launched! Monitoring logs (Ctrl+C to stop)..."
echo "📱 Check the emulator window to see the app"
echo ""

# Monitor logs
adb logcat -s Udroid:V AndroidRuntime:E
