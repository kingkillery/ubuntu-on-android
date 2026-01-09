# Android Emulator Setup Guide

## Option 1: Android Studio (GUI - Recommended)

### Step 1: Launch Android Studio
```bash
open -a "Android Studio"
# Or from terminal
studio
```

### Step 2: Initial Setup
1. **First Launch:** Follow the setup wizard
2. **SDK Setup:** Point to existing SDK at `/opt/homebrew/share/android-commandlinetools`
   - Go to: `Android Studio > Settings > Appearance & Behavior > System Settings > Android SDK`
   - Set SDK location: `/opt/homebrew/share/android-commandlinetools`

### Step 3: Create Virtual Device
1. Click **More Actions** (or Tools menu)
2. Select **Virtual Device Manager** (or **AVD Manager**)
3. Click **Create Device**
4. Choose a device definition:
   - **Recommended:** Pixel 6 Pro (6.7", 1440x3120, arm64)
   - Or: Any device with API 26+ support
5. Click **Next**
6. Select a System Image:
   - **Download** API Level 34 (Android 14) - arm64 architecture
   - Or API Level 33/32/31 (Android 13/12)
   - Make sure to choose **arm64** or **x86_64** (not x86)
7. Click **Next**
8. Configure AVD:
   - Name: `Ubuntu_Test_Device`
   - Startup orientation: Portrait
   - Enable Device Frame
9. Click **Finish**

### Step 4: Launch Emulator
1. In AVD Manager, click the **Play ▶** button next to your device
2. Wait for emulator to boot (30-60 seconds first time)
3. Emulator window will appear with Android home screen

### Step 5: Install and Test APK
```bash
# Wait for emulator to fully boot (check with adb devices)
adb devices

# Install the APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch the app
adb shell am start -n com.udroid.app.debug/.MainActivity

# Monitor logs in real-time
adb logcat -s Udroid:V AndroidRuntime:E
```

---

## Option 2: Command-Line Emulator (Advanced)

### Step 1: Install Emulator and System Image
```bash
# Set environment
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"

# Install emulator
sdkmanager "emulator" "system-images;android-34;google_apis;arm64-v8a"

# Accept licenses
sdkmanager --licenses
```

### Step 2: Create AVD via Command Line
```bash
# Create AVD
avdmanager create avd \
  --name Ubuntu_Test \
  --package "system-images;android-34;google_apis;arm64-v8a" \
  --device "pixel_6_pro"

# List available AVDs
avdmanager list avd
```

### Step 3: Launch Emulator
```bash
# Launch emulator
$ANDROID_HOME/emulator/emulator -avd Ubuntu_Test &

# Or with GPU acceleration
$ANDROID_HOME/emulator/emulator -avd Ubuntu_Test -gpu host &

# Wait for boot
adb wait-for-device
```

### Step 4: Install and Test
```bash
# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.udroid.app.debug/.MainActivity

# View logs
adb logcat -s Udroid:V AndroidRuntime:E
```

---

## Quick Test Script

Save this as `test-on-emulator.sh`:

```bash
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

# Install APK
echo "📦 Installing APK..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
echo "🎯 Launching Ubuntu on Android..."
adb shell am start -n com.udroid.app.debug/.MainActivity

echo ""
echo "✅ App launched! Monitoring logs (Ctrl+C to stop)..."
echo ""

# Monitor logs
adb logcat -s Udroid:V AndroidRuntime:E
```

Make it executable:
```bash
chmod +x test-on-emulator.sh
```

---

## Troubleshooting

### Emulator Won't Start
```bash
# Check if emulator is installed
ls $ANDROID_HOME/emulator/emulator

# Check available system images
sdkmanager --list | grep system-images

# Check AVD configuration
avdmanager list avd
```

### Emulator is Slow
1. **Enable Hardware Acceleration:**
   - macOS: Ensure Hypervisor.framework is enabled (built-in)
   - Check: Android Studio > Settings > Tools > Emulator
   - Enable: "Launch in a tool window" for better performance

2. **Allocate More Resources:**
   - Edit AVD Settings
   - Increase RAM to 4GB+
   - Increase VM Heap to 512MB+

3. **Use arm64 System Image:**
   - arm64 images run natively on Apple Silicon Macs
   - Much faster than x86 emulation

### App Crashes on Launch
```bash
# View crash logs
adb logcat AndroidRuntime:E *:S

# Check app process
adb shell ps | grep udroid

# Clear app data and retry
adb shell pm clear com.udroid.app.debug
adb shell am start -n com.udroid.app.debug/.MainActivity
```

### Permission Issues
```bash
# Grant permissions via ADB
adb shell pm grant com.udroid.app.debug android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.udroid.app.debug android.permission.WRITE_EXTERNAL_STORAGE
adb shell pm grant com.udroid.app.debug android.permission.INTERNET
```

---

## Testing Checklist

Once the app launches, test the following:

### Basic Functionality
- [ ] App launches without crash
- [ ] Setup wizard appears
- [ ] Can select Ubuntu distribution (Jammy/Focal)
- [ ] Can select desktop environment (XFCE/MATE)
- [ ] Can enter session name

### Permissions
- [ ] Storage permission dialog appears
- [ ] Network permission dialog appears
- [ ] Permissions can be granted

### Download Flow (if implemented)
- [ ] Download button is clickable
- [ ] Progress indicator appears
- [ ] Download completes (or shows error)

### Session Management
- [ ] Session can be created
- [ ] Session appears in list
- [ ] Session persists after app restart

### Logs to Watch For
```bash
# Success indicators
"UdroidApplication created"
"MainActivity created"
"SetupWizardViewModel initialized"

# Error indicators (need fixing)
"FATAL EXCEPTION"
"NullPointerException"
"ActivityNotFoundException"
```

---

## Performance Tips

### For Apple Silicon Macs (M1/M2/M3)
- ✅ Use arm64-v8a system images (native, very fast)
- ⚠️ Avoid x86/x86_64 images (requires translation, slow)

### Recommended AVD Settings
- **RAM:** 4096 MB
- **VM Heap:** 512 MB
- **Internal Storage:** 2048 MB
- **SD Card:** 512 MB (for rootfs testing)
- **Graphics:** Hardware (GLES 2.0)

---

## Next Steps After Emulator Setup

1. **Test Basic Flow:**
   - Launch app → Grant permissions → See setup wizard

2. **Test Session Creation:**
   - Create a test session → Verify it appears in list

3. **Debug Issues:**
   - Use `adb logcat` to see runtime errors
   - Fix crashes and null pointer exceptions
   - Implement missing functionality (PRoot, VNC)

4. **Iterate:**
   - Fix bugs → Rebuild → Test again
   - Use Ralph (if working) for autonomous fixes

---

**Quick Start (TL;DR):**
```bash
# 1. Launch Android Studio
open -a "Android Studio"

# 2. Create AVD with Pixel 6 Pro + Android 14 (arm64)

# 3. Start emulator from AVD Manager

# 4. Install and test
./test-on-emulator.sh
```

Good luck testing! 🚀
