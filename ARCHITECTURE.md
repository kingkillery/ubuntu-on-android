# Ubuntu on Android - Standalone App Architecture

## Executive Summary

This document outlines the transformation of **udroid** (Termux-based Ubuntu PRoot environment) into a **native Android application** that provides a complete Ubuntu desktop experience within an app sandbox.

### Current State (udroid/Termux)
- **Installation**: Bash script (`install.sh`) that clones `fs-manager-udroid` repo
- **Runtime**: Requires Termux app + PRoot for chroot-like environment
- **Distribution**: Pre-built Ubuntu rootfs tarballs (jammy, focal, etc.) hosted on GitHub releases
- **Desktop**: VNC server (tightvnc/tigervnc) + XFCE/MATE/GNOME desktop environments
- **Dependencies**: proot, proot-distro, pulseaudio, wget, jq, pv

### Target State (Native Android App)
- **Installation**: Single APK from Play Store or direct download
- **Runtime**: Native Android app managing PRoot environment directly
- **Distribution**: In-app rootfs download with delta updates
- **Desktop**: Native Android Surface rendering with gesture support
- **Dependencies**: Bundled native libraries + app-managed lifecycle

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     Android Application Layer                    │
├─────────────────────────────────────────────────────────────────┤
│  UI Layer:                                                      │
│  • MainActivity (permissions, setup wizard)                     │
│  • SessionListActivity (manage multiple Ubuntu instances)       │
│  • DesktopActivity (full-screen desktop view)                   │
│  • SettingsFragment (configuration, storage, updates)           │
├─────────────────────────────────────────────────────────────────┤
│  Service Layer:                                                 │
│  • UbuntuSessionService (lifecycle management)                  │
│  • RootfsDownloadService (download, verify, extract)            │
│  • VncBridgeService (connects VNC to Android Surface)           │
│  • LogMonitorService (capture and surface logs)                 │
├─────────────────────────────────────────────────────────────────┤
│  Native Layer (JNI):                                            │
│  • libproot-android (PRoot compiled for Android)                │
│  • libvncbridge (VNC → Android Surface translation)             │
│  • libtermux-exec (command execution bridge)                    │
│  • libinput-bridge (touch/keyboard → X11 events)                │
├─────────────────────────────────────────────────────────────────┤
│  Filesystem Layer:                                              │
│  • /data/data/com.udroid.app/files/ubuntu/ (rootfs)            │
│  • /data/data/com.udroid.app/files/sessions/ (per-instance)     │
│  • /data/data/com.udroid.app/cache/downloads/ (rootfs tarballs)│
└─────────────────────────────────────────────────────────────────┘
```

---

## Component Breakdown

### 1. Android App Shell

**Responsibilities:**
- Request and manage permissions (storage, network, notification)
- Manage app lifecycle (foreground service for sessions)
- Provide setup wizard for first-time users
- Expose session management APIs (start, stop, monitor status)

**Key Classes:**
```kotlin
// Core session management
class UbuntuSessionManager {
    fun createSession(config: SessionConfig): UbuntuSession
    fun listSessions(): List<UbuntuSession>
    fun getSession(id: String): UbuntuSession?
}

class UbuntuSession {
    val id: String
    val distro: DistroVariant
    val state: SessionState
    fun start()
    fun stop()
    fun exec(command: String): ProcessResult
}

// Service layer
class UbuntuSessionService : Service() {
    private val sessions = mutableMapOf<String, SessionProcess>()
    fun startSession(sessionId: String)
    fun stopSession(sessionId: String)
}

// UI Layer
class MainActivity : AppCompatActivity() {
    fun onRequestPermissions()
    fun navigateToSetup()
    fun showSessionList()
}

class DesktopActivity : AppCompatActivity() {
    private lateinit var surfaceView: DesktopSurfaceView
    private lateinit var inputHandler: DesktopInputHandler
    fun connectToSession(sessionId: String)
}
```

**Permission Requirements:**
- `android.permission.INTERNET` (rootfs downloads, networking)
- `android.permission.WRITE_EXTERNAL_STORAGE` (rootfs storage)
- `android.permission.FOREGROUND_SERVICE` (run Ubuntu in background)
- `android.permission.WAKE_LOCK` (prevent sleep during desktop session)
- `android.permission.POST_NOTIFICATIONS` (session status updates)

---

### 2. Linux/PRoot Integration

**Adaptation Strategy:**
The current udroid scripts rely on:
1. **proot-distro** - Manages rootfs installation and login
2. **bash scripts** - Install, login, and configuration logic
3. **termux-exec** - Handles command execution in proot environment

**Android Adaptation:**

```
Original (Termux):
  → Termux app provides bash environment
  → proot-distro installs rootfs to ~/.proot-distro/installed-rootfs/
  → udroid login jammy:xfce4 launches bash script
  → User manually starts vncserver :1
  → User connects via VNC viewer app

Target (Android App):
  → App provides bash via native binary (bundled)
  → App manages rootfs in app-specific files directory
  → UbuntuSessionService.start() launches proot + init
  → VNC bridge service auto-starts with session
  → DesktopActivity renders VNC to native Surface
```

**Key Components:**

**A. Native Binary Bundling**
```
app/src/main/jni/
  ├── proot/           # PRoot compiled for Android (AArch64, ARMv7, x86_64)
  ├── bash/            # Bash shell binary
  ├── busybox/         # Core utilities
  ├── proot-distro/    # Modified proot-distro for Android paths
  └── lib/             # Native libraries (.so files)
      ├── libproot-android.so
      ├── libvncbridge.so
      └── libtermux-exec.so
```

**B. Rootfs Management**
```kotlin
class RootfsManager {
    private val rootfsDir = File(context.filesDir, "ubuntu/rootfs")
    private val cacheDir = File(context.cacheDir, "downloads")

    suspend fun downloadRootfs(
        distro: DistroVariant,
        onProgress: (Int) -> Unit
    ): Result<File>

    fun verifyRootfs(file: File, checksum: String): Boolean
    fun extractRootfs(archive: File): Result<Unit>
    fun getInstalledRootfs(): List<DistroVariant>
    fun deleteRootfs(distro: DistroVariant)
}

enum class DistroVariant(val id: String, val downloadUrl: String) {
    JAMMY_XFCE4("jammy:xfce4", "..."),
    JAMMY_MATE("jammy:mate", "..."),
    JAMMY_GNOME("jammy:gnome", "..."),
    NOBLE_RAW("noble:raw", "...")
}
```

**C. Session Launch Pipeline**
```kotlin
class UbuntuSessionManager {
    fun startSession(session: UbuntuSession) {
        // 1. Verify rootfs exists
        if (!rootfsManager.isInstalled(session.distro)) {
            throw RootfsNotInstalledException()
        }

        // 2. Prepare session directory
        val sessionDir = prepareSessionDir(session.id)

        // 3. Generate proot command
        val prootCmd = buildProotCommand(
            rootfs = rootfsManager.getRootfsPath(session.distro),
            sessionDir = sessionDir,
            env = buildEnvVars(session)
        )

        // 4. Launch native process via JNI
        nativeProcess = launchProotProcess(prootCmd)

        // 5. Wait for VNC server to start
        waitForVncServer(sessionDir)

        // 6. Notify UI ready
        session.state = SessionState.RUNNING
    }

    private fun buildProotCommand(
        rootfs: File,
        sessionDir: File,
        env: Map<String, String>
    ): List<String> = listOf(
        "proot",
        "--rootfs", rootfs.absolutePath,
        "--bind", "$sessionDir:/home/ubuntu_session",
        "--bind", "/sdcard:/mnt/sdcard",
        "--kill-on-exit",
        "/bin/bash", "-c", "export DISPLAY=:1 && vncserver :1 && tail -f /dev/null"
    )
}
```

---

### 3. GUI & Display Pipeline

**Display Strategy Options:**

**Option A: VNC Bridge (Recommended for V1)**
```
VNC Server (in proot) → libvncbridge.so → Android SurfaceView
```
- **Pros**: Reuses existing udroid setup, stable
- **Cons**: Network overhead, extra process

**Option B: Wayland Direct (Future)**
```
Wayland compositor → libwayland-android.so → Android Surface
```
- **Pros**: Better performance, native feel
- **Cons**: Requires Wayland setup, more complex

**Option C: XServer Android (Hybrid)**
```
XOrg server → libxorg-android.so → Android SurfaceControl
```
- **Pros**: Full X11 compatibility
- **Cons**: Complex native integration

**VNC Bridge Implementation:**
```kotlin
class VncBridgeService : Service() {
    private lateinit var vncClient: VncClient
    private lateinit var surfaceRenderer: SurfaceRenderer

    fun connectToSession(sessionId: String, host: String, port: Int) {
        vncClient = VncClient(host, port)
        vncClient.setFramebufferCallback { buffer ->
            surfaceRenderer.updateFrame(buffer)
        }
        vncClient.connect()
    }

    fun sendInputEvent(event: InputEvent) {
        when (event) {
            is TouchEvent -> vncClient.sendPointerEvent(event.x, event.y, event.mask)
            is KeyEvent -> vncClient.sendKeyEvent(event.keyCode, event.isDown)
        }
    }
}

class DesktopSurfaceView : SurfaceView {
    private val renderer = SurfaceRenderer()
    private val inputHandler = DesktopInputHandler()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val vncEvent = inputHandler.translateTouchEvent(event)
        vncBridgeService.sendInputEvent(vncEvent)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val vncEvent = inputHandler.translateKeyEvent(event)
        vncBridgeService.sendInputEvent(vncEvent)
        return true
    }
}
```

**Input Translation:**
```kotlin
class DesktopInputHandler {
    // Touch → Mouse pointer
    fun translateTouchEvent(event: MotionEvent): PointerEvent {
        return PointerEvent(
            x = event.x.toInt(),
            y = event.y.toInt(),
            mask = when (event.action) {
                MotionEvent.ACTION_DOWN -> MouseEventMask.LEFT_BUTTON_DOWN
                MotionEvent.ACTION_UP -> MouseEventMask.LEFT_BUTTON_UP
                MotionEvent.ACTION_MOVE -> MouseEventMask.EMPTY
                else -> MouseEventMask.EMPTY
            }
        )
    }

    // Pinch → Scroll wheel
    fun translateGesture(detector: ScaleGestureDetector): ScrollEvent {
        return ScrollEvent(
            deltaX = detector.currentSpan - detector.previousSpan,
            deltaY = 0
        )
    }

    // Keyboard → X11 keysyms
    fun translateKeyEvent(event: KeyEvent): KeyEvent {
        val keysym = keyMap.getKeySym(event.keyCode)
        return KeyEvent(
            keysym = keysym,
            isDown = event.action == KeyEvent.ACTION_DOWN
        )
    }
}
```

---

### 4. Packaging, Storage & Updates

**Rootfs Distribution:**

**Phase 1: On-Demand Download (V1.0)**
- App contains no rootfs (keeps APK small ~20MB)
- User selects distro in setup wizard
- Background download with progress + pause/resume
- SHA256 verification after download
- Extract to app-specific files directory

**Phase 2: Bundled Base Rootfs (V1.5)**
- Minimal Ubuntu base (~500MB) bundled in APK splits
- Full desktop environments downloaded separately
- Reduces initial download time

**Phase 3: Delta Updates (V2.0)**
- Use rsync-like algorithm for incremental updates
- Only download changed files between versions
- Signature verification for security

**Storage Layout:**
```
/data/data/com.udroid.app/
├── files/
│   ├── ubuntu/
│   │   ├── rootfs/
│   │   │   ├── jammy-xfce4/         # Ubuntu 22.04 + XFCE4
│   │   │   │   ├── bin/
│   │   │   │   ├── lib/
│   │   │   │   ├── usr/
│   │   │   │   └── etc/
│   │   │   └── noble-raw/           # Ubuntu 24.04 minimal
│   │   └── sessions/
│   │       ├── session-abc123/
│   │       │   ├── .vnc/            # VNC passwords, configs
│   │       │   ├── home/
│   │       │   └── config.json
│   └── native/
│       ├── proot
│       ├── bash
│       └── proot-distro/
├── cache/
│   └── downloads/
│       └── jammy-xfce4-arm64.tar.gz (may be partially downloaded)
└── shared_prefs/
    └── sessions.xml                  # Session metadata
```

**Download Service:**
```kotlin
class RootfsDownloadService : Service() {
    private lateinit var downloadManager: DownloadManager

    fun startDownload(distro: DistroVariant, url: String) {
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setDestinationInExternalFilesDir(
                this@RootfsDownloadService,
                Environment.DIRECTORY_DOWNLOADS,
                "${distro.id}.tar.gz"
            )
            setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or
                DownloadManager.Request.NETWORK_MOBILE
            )
            setTitle("Downloading Ubuntu ${distro.id}")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        }

        downloadId = downloadManager.enqueue(request)
    }

    private fun onDownloadComplete(downloadId: Long) {
        // Verify SHA256
        val file = queryDownloadedFile(downloadId)
        if (!verifyChecksum(file, distro.sha256)) {
            notifyError("Checksum verification failed")
            return
        }

        // Extract rootfs
        lifecycleScope.launch {
            val progress = extractRootfs(file)
            notificationManager.updateProgress(progress)
        }
    }
}
```

**Size Optimization:**
- Use APK splits for different architectures (arm64-v8a, armeabi-v7a, x86_64)
- Compress rootfs tarballs with gzip (already done)
- Consider zstd for better compression in V2
- Allow users to remove unused distros

---

### 5. Developer Experience & Automation

**Build Tooling:**

**A. Build System (Gradle)**
```gradle
// app/build.gradle
android {
    defaultConfig {
        ndk {
            abiFilters 'arm64-v8a', 'armeabi-v7a', 'x86_64'
        }
    }

    sourceSets {
        main {
            jniLibs.srcDirs = ['src/main/jni/lib']
        }
    }
}

dependencies {
    implementation 'androidx.lifecycle:lifecycle-service:2.7.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'com.squareup.retrofit2:retrofit:2.9.0' // For updates API
}
```

**B. Native Library Build Script**
```bash
#!/bin/bash
# build-native.sh

# Cross-compile PRoot for Android
mkdir -p build/proot
cd build/proot
cmake -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
      -DANDROID_ABI=arm64-v8a \
      -DANDROID_PLATFORM=android-24 \
      ../../native/proot
make -j$(nproc)

# Build VNC bridge
cd ../vnc-bridge
$NDK/ndk-build NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=./Android.mk

# Copy to jniLibs
mkdir -p ../../app/src/main/jniLibs/arm64-v8a
cp *.so ../../app/src/main/jniLibs/arm64-v8a/
```

**C. CI/CD Pipeline (GitHub Actions)**
```yaml
# .github/workflows/build.yml
name: Build Ubuntu on Android

on:
  push:
    branches: [main]
  release:
    types: [created]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Android SDK
        uses: android-actions/setup-android@v2

      - name: Build native libraries
        run: ./scripts/build-native.sh

      - name: Build APK
        run: ./gradlew assembleRelease

      - name: Sign APK
        run: ./scripts/sign-apk.sh

      - name: Upload artifact
        uses: actions/upload-artifact@v3
        with:
          name: udroid-apk
          path: app/build/outputs/apk/release/*.apk
```

**D. Testing Infrastructure**
```kotlin
// Example unit test for session management
class UbuntuSessionManagerTest {
    @Test
    fun `startSession should fail when rootfs not installed`() = runTest {
        val manager = UbuntuSessionManager(mockContext)
        val session = UbuntuSession(id = "test", distro = JAMMY_XFCE4)

        assertFailsWith<RootfsNotInstalledException> {
            manager.startSession(session)
        }
    }

    @Test
    fun `startSession should launch proot process`() = runTest {
        // Mock rootfs exists
        every { mockRootfsManager.isInstalled(JAMMY_XFCE4) } returns true

        val manager = UbuntuSessionManager(mockContext)
        val session = UbuntuSession(id = "test", distro = JAMMY_XFCE4)

        manager.startSession(session)

        verify { nativeProcessLauncher.launch(any()) }
    }
}
```

**E. Mock Rootfs for Development**
```bash
#!/bin/bash
# scripts/create-mock-rootfs.sh

# Create minimal fake rootfs for UI testing
MOCK_DIR=app/src/debug/assets/mock-rootfs
mkdir -p $MOCK_DIR/{bin,lib,usr,etc,home}

# Mock binaries (echo binaries that pretend to be real)
echo '#!/bin/bash' > $MOCK_DIR/bin/bash
echo 'echo "Mock bash running"' >> $MOCK_DIR/bin/bash
chmod +x $MOCK_DIR/bin/bash

# Create fake proot that just returns success
cat > $MOCK_DIR/bin/proot << 'EOF'
#!/bin/bash
echo "Mock proot: $@" > /tmp/proot.log
sleep 1
echo "Mock proot exited successfully"
EOF
chmod +x $MOCK_DIR/bin/proot
```

---

## Task Dependency Graph

```
┌─────────────────────────────────────────────────────────────────────┐
│ Phase 1: Foundation (Can run in parallel after architecture)        │
├─────────────────────────────────────────────────────────────────────┤
│ [A1] Android app scaffold + permissions        │
│ [A2] Native library build setup (CMake, NDK)   │
│ [A3] Rootfs download + verification logic     │
│ [A4] Basic UI (MainActivity, setup wizard)    │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│ Phase 2: Core Integration (Sequential dependencies)                 │
├─────────────────────────────────────────────────────────────────────┤
│ [B1] Port PRoot to Android                         │
│ [B2] Adapt proot-distro scripts for Android paths  │
│ [B3] Implement UbuntuSessionService               │
│ [B4] Rootfs extraction and mounting                │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│ Phase 3: Display & Input (Parallel after B3)                        │
├─────────────────────────────────────────────────────────────────────┤
│ [C1] VNC bridge native library                  │
│ [C2] DesktopSurfaceView rendering               │
│ [C3] Input translation (touch → mouse)          │
│ [C4] Keyboard handling (IME → X11 keysyms)      │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│ Phase 4: Polish & DX (Parallel after C1-C4)                         │
├─────────────────────────────────────────────────────────────────────┤
│ [D1] Session persistence & state management   │
│ [D2] Error handling & user feedback            │
│ [D3] CI/CD pipeline                             │
│ [D4] Testing infrastructure                    │
│ [D5] Documentation                              │
└─────────────────────────────────────────────────────────────────────┘
```

**Critical Path:**
Architecture → A2 (Native build setup) → B1 (PRoot port) → B3 (Session service) → C1 (VNC bridge) → C2 (Surface view)

**Parallelizable Streams:**
- **Stream 1**: Android UI (A4 → B3 → D2)
- **Stream 2**: Native integration (A2 → B1 → B4 → C1)
- **Stream 3**: Rootfs management (A3 → B4 → D1)
- **Stream 4**: Input handling (A4 → C3 → C4)
- **Stream 5**: Developer tooling (D3 → D4 → D5)

---

## Synchronization Points

### Sync Point 1: Rootfs Layout Lock-in
**When**: After Phase 1, before Phase 2 begins
**Decision**: Final rootfs directory structure and naming conventions
**Blocks**: All rootfs-related code (download, extraction, mounting)

### Sync Point 2: Display Strategy Decision
**When**: Before Phase 3 begins
**Decision**: VNC bridge vs. Wayland vs. XServer
**Blocks**: GUI implementation, input handling

### Sync Point 3: Session API Contract
**When**: After B3 (Session service)
**Decision**: Public API for `UbuntuSessionManager` and `UbuntuSession`
**Blocks**: UI integration, testing

---

## Open Questions & Research Items

1. **PRoot on Android**: Has PRoot been tested without Termux? Any Android-specific patches needed?
2. **VNC Performance**: Can we achieve 30+ FPS with VNC bridge on mid-range devices?
3. **Storage Constraints**: How to handle devices with limited internal storage?
4. **Multi-window**: Support Android multi-window mode (split-screen)?
5. **Security**: Sandbox isolation between Ubuntu instance and Android system?
6. **Licensing**: PRoot is GPL2 - how does this impact app distribution?

---

## Next Steps

1. **Create agent charters** for each sub-team
2. **Setup Android project** with basic scaffold
3. **Prototype PRoot on Android** (prove feasibility)
4. **Define interface contracts** between components
5. **Begin parallel implementation** of Phase 1 tasks
