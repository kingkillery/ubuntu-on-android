# Agent 3: Linux/PRoot Integration

## Mission

Adapt the existing udroid PRoot-based environment to run within an Android application context, replacing Termux dependencies with Android-native implementations.

## Scope

**In Scope:**
- PRoot compilation for Android (ARM64, ARMv7, x86_64)
- Adaptation of proot-distro scripts for Android filesystem layout
- Rootfs installation and management logic
- Session startup/shutdown scripts
- Environment configuration for Android context
- Native bridge JNI implementation

**Out of Scope:**
- VNC server configuration (handled by GUI agent)
- Desktop environment setup (part of rootfs tarball)
- Android UI implementation

## Key Responsibilities

### PRoot Android Port
- Cross-compile PRoot for Android architectures
- Test PRoot in Android app context
- Identify and fix Android-specific issues (seccomp, mount namespaces)
- Package as native library (.so)

### proot-distro Adaptation
- Modify proot-distro to use Android paths instead of Termux
- Update rootfs installation scripts for app directories
- Adapt login scripts for Android launch mechanism

### Rootfs Management
- Rootfs download verification logic
- Extraction to app-specific directories
- Upgrade/repair logic
- Multi-distro support

### Session Orchestration
- Build PRoot command line for session startup
- Environment variable setup (DISPLAY, HOME, PATH)
- Mount point configuration
- Process lifecycle management (start, monitor, kill)

### Native JNI Bridge
- JNI functions for process launching
- stdio redirection (capture stdout/stderr)
- Signal handling (SIGTERM, SIGKILL)
- Exit code propagation

## Authoritative Files

```
app/src/main/java/com/udroid/app/nativebridge/NativeBridge.kt
app/src/main/java/com/udroid/app/session/UbuntuSessionManagerImpl.kt
app/src/main/java/com/udroid/app/rootfs/RootfsManager.kt
native/CMakeLists.txt
scripts/build-native.sh
```

## Sync Points

**Dependencies:**
- Requires rootfs layout decisions from Agent 1
- Requires native build setup from Agent 2

**Coordinates With:**
- Agent 4: Session startup → VNC server launch coordination
- Agent 5: Rootfs download URL and verification scheme

**Blocks:**
- GUI implementation depends on session startup API
