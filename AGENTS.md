# Agent Charters - Ubuntu on Android App

This document defines the scope, responsibilities, and deliverables for each specialist sub-agent working on the Ubuntu-on-Android standalone app transformation.

---

## Agent 1: Requirements & Architecture Agent

### Mission
Define product requirements and high-level technical architecture for transforming udroid (Termux-based) into a standalone Android application that runs Ubuntu in an app sandbox.

### Scope

**In Scope:**
- Product requirements document (PRD) defining target user experience
- High-level system architecture and component diagram
- Technology stack decisions (display pipeline, storage model, etc.)
- API contracts and interface definitions between components
- Migration strategy from Termux-based to native Android

**Out of Scope:**
- Implementation details (delegated to other agents)
- UI/UX visual design (functional flows only)
- Specific library choices unless architecturally significant

### Key Responsibilities

1. **Requirements Definition**
   - Define offline/online behavior scenarios
   - Specify storage model and constraints
   - Document expected UX flows (first-run, session management, error recovery)
   - Identify device compatibility requirements (Android version, RAM, storage)

2. **Architecture Design**
   - Design high-level architecture: Android front-end, native layer, PRoot integration, display pipeline
   - Define component boundaries and interfaces
   - Specify data flow and communication patterns
   - Identify security and isolation requirements

3. **Technology Decisions**
   - Choose display strategy (VNC/Wayland/XServer) with tradeoff analysis
   - Define rootfs distribution model (bundled/downloaded/delta)
   - Specify networking approach (bridge, NAT, direct)
   - Select persistence layer for session state

4. **Interface Contracts**
   - Define public APIs for `UbuntuSessionManager`
   - Specify `UbuntuSession` lifecycle interface
   - Document native bridge JNI interfaces
   - Define configuration file formats

### Deliverables

- [ ] **Product Requirements Document (PRD)**
  - Target user personas
  - Use cases and user stories
  - Functional requirements
  - Non-functional requirements (performance, security, compatibility)

- [ ] **System Architecture Document**
  - Component diagram
  - Layer architecture (UI, Service, Native, Filesystem)
  - Data flow diagrams
  - Security boundaries

- [ ] **Interface Specification**
  - `UbuntuSessionManager` API (Kotlin interfaces)
  - `UbuntuSession` lifecycle contract
  - JNI bridge signatures
  - Configuration schema (JSON, properties files)

- [ ] **Technology Decision Record**
  - Display pipeline choice with justification
  - Rootfs distribution strategy
  - Storage architecture decision
  - Build tooling approach

- [ ] **Migration Strategy**
  - Analysis of existing udroid/Termux components
  - Mapping from Termux paths to Android paths
  - Compatibility layer design

### Sync Points

**Must Coordinate With:**
- **Android App Shell Agent**: Before finalizing session management APIs
- **Linux/PRoot Integration Agent**: Before finalizing filesystem layout
- **GUI & Interaction Agent**: Before choosing display strategy

**Output Dependencies:**
- Android app scaffold depends on architecture decisions
- All other agents depend on interface specifications

---

## Agent 2: Android App Shell Agent

### Mission
Design and implement the Android application framework that manages Ubuntu sessions, handles permissions, provides user interface, and exposes integration points for the native Ubuntu environment.

### Scope

**In Scope:**
- Android app architecture (single-activity Compose app + foreground service)
- Permission request flows
- Session management UI
- Settings and configuration UI
- Foreground service for background session management
- Integration hooks for native components

**Out of Scope:**
- Native library implementation ( JNI interfaces only)
- Desktop rendering implementation (SurfaceView stubs only)
- Rootfs download implementation (service stubs only)

### Key Responsibilities

1. **App Architecture**
   - Maintain single-activity Compose architecture (`MainActivity` + `NavHost`)
   - Maintain navigation routes and arguments (`session_list`, `setup_wizard`, `desktop/{sessionId}`)
   - Maintain dependency injection (Hilt) wiring for ViewModels and services
   - Ensure UI observes `Flow`/`StateFlow` without manual lifecycle plumbing

2. **Permission Management**
   - Request storage, network, foreground service permissions
   - Handle permission denial gracefully
   - Show explanatory UI for permission rationale

3. **Session Management UI**
   - Session list screen: create/delete/start/stop sessions
   - Setup wizard screen: distro selection + session name + create session
   - Desktop screen: placeholder surface for future rendering, stop control, basic state display

4. **Settings & Configuration**
   - App settings (theme, notifications, storage)
   - Per-session configuration (desktop environment, resolution)
   - Network settings (proxy, VNC port)
   - Storage management (view usage, delete rootfs)

5. **Background Services**
   - `UbuntuSessionService` for foreground session management
   - Notification channel for session status
   - Service lifecycle management (bind, start, stop)

### Deliverables

- [ ] **Android Project Scaffold**
  - Gradle build configuration
  - AndroidManifest with permissions
  - Base activity and application classes
  - Dependency injection setup

- [ ] **MainActivity + Navigation**
  - Permissions gate and request flow
  - Compose `NavHost` routes and arguments
  - Correct back stack behavior when setup completes

- [ ] **SetupWizard (Compose)**
  - `ui/setup/SetupWizardScreen.kt` + `SetupWizardViewModel.kt`
  - Create session via `UbuntuSessionManager.createSession(config)`

- [ ] **SessionList (Compose)**
  - `ui/session/SessionListScreen.kt` + `SessionListViewModel.kt`
  - Observe sessions via `UbuntuSessionManager.listSessions(): Flow<List<UbuntuSession>>`
  - Start/stop/delete actions using `Result`-based APIs

- [ ] **Desktop (Compose)**
  - `ui/desktop/DesktopScreen.kt` + `DesktopViewModel.kt`
  - Placeholder rendering surface (to be replaced by VNC/Wayland pipeline)

- [ ] **Settings (Future)**
  - Keep as planned work; no Settings screen exists yet in the Compose nav graph

- [ ] **UbuntuSessionService**
  - Service implementation
  - Session lifecycle management
  - Notification support
  - Binder interface for UI communication

- [ ] **ViewModels**
  - `SessionListViewModel` for managing session state
  - `SetupWizardViewModel` for setup flow
  - `DesktopViewModel` for desktop session state

### Sync Points

**Dependencies:**
- Requires architecture decisions from Requirements & Architecture Agent
- Requires interface definitions for `UbuntuSessionManager`
- Coordinates with Linux/PRoot Integration Agent for native bridge calls

**Blocks:**
- GUI implementation depends on the desktop rendering pipeline (current `DesktopScreen` is a placeholder)
- Testing depends on service and ViewModel completion

### Current Code Touchpoints (Authoritative)

- `app/src/main/java/com/udroid/app/MainActivity.kt` (permissions + Compose navigation)
- `app/src/main/java/com/udroid/app/ui/session/SessionListScreen.kt`
- `app/src/main/java/com/udroid/app/ui/session/SessionListViewModel.kt`
- `app/src/main/java/com/udroid/app/ui/setup/SetupWizardScreen.kt`
- `app/src/main/java/com/udroid/app/ui/setup/SetupWizardViewModel.kt`
- `app/src/main/java/com/udroid/app/ui/desktop/DesktopScreen.kt` (placeholder)
- `app/src/main/java/com/udroid/app/service/UbuntuSessionService.kt`

---

## Agent 3: Linux/PRoot Integration Agent

### Mission
Adapt the existing udroid PRoot-based environment to run within an Android application context, replacing Termux dependencies with Android-native implementations while maintaining compatibility with Ubuntu rootfs.

### Scope

**In Scope:**
- PRoot compilation for Android (ARM64, ARMv7, x86_64)
- Adaptation of proot-distro scripts for Android filesystem layout
- Rootfs installation and management logic
- Session startup/shutdown scripts
- Environment configuration for Android context
- Native bridge JNI implementation

**Out of Scope:**
- VNC server configuration (handled by GUI agent or scripts in rootfs)
- Desktop environment setup (part of rootfs tarball)
- Android UI implementation

### Key Responsibilities

1. **PRoot Android Port**
   - Cross-compile PRoot for Android architectures
   - Test PRoot functionality in Android app context
   - Identify and fix Android-specific issues (seccomp, mount namespaces)
   - Package as native library (.so)

2. **proot-distro Adaptation**
   - Modify proot-distro to use Android paths instead of Termux
   - Update rootfs installation scripts for app directories
   - Adapt login scripts for Android launch mechanism
   - Test script execution via JNI

3. **Rootfs Management**
   - Implement rootfs download verification logic
   - Rootfs extraction to app-specific directories
   - Rootfs upgrade/repair logic
   - Multi-distro support (install multiple variants)

4. **Session Orchestration**
   - Build PRoot command line for session startup
   - Environment variable setup (DISPLAY, HOME, PATH)
   - Mount point configuration (bind mounts for Android storage)
   - Process lifecycle management (start, monitor, kill)

5. **Native JNI Bridge**
   - Implement JNI functions for process launching
   - Handle stdio redirection (capture stdout/stderr)
   - Signal handling (SIGTERM, SIGKILL for session shutdown)
   - Exit code propagation

### Deliverables

- [ ] **PRoot for Android**
  - Compiled binaries for arm64-v8a, armeabi-v7a, x86_64
  - Build script (CMake/NDK)
  - Test suite validating PRoot functionality

- [ ] **Modified proot-distro**
  - Adapted scripts for Android paths
  - Configuration file for app rootfs directory
  - Installation script compatible with JNI execution

- [ ] **RootfsInstaller** (Kotlin/JNI)
  - Download with resume support
  - SHA256 verification
  - Extraction to app files directory
  - Installation status tracking

- [ ] **UbuntuSessionManager** (Native + Kotlin)
  - JNI bridge for session lifecycle
  - Process management (launch, monitor, terminate)
  - Environment setup
  - Log capture and forwarding

- [ ] **Configuration System**
  - Session configuration files (JSON)
  - Environment variable templates
  - PRoot option builders

- [ ] **Testing Suite**
  - Unit tests for RootfsInstaller
  - Integration tests for session lifecycle
  - Mock rootfs for CI testing

### Sync Points

**Dependencies:**
- Requires rootfs layout decisions from Requirements & Architecture Agent
- Requires native build setup from Android App Shell Agent

**Coordinates With:**
- **GUI Agent**: Session startup → VNC server launch coordination
- **Packaging Agent**: Rootfs download URL and verification scheme

**Blocks:**
- GUI implementation depends on session startup API
- End-to-end testing depends on session management completion

---

## Agent 4: GUI & Interaction Agent

### Mission
Implement the desktop display pipeline and input handling, providing a native Android surface rendering of the Ubuntu desktop environment with touch, keyboard, and gesture support.

### Scope

**In Scope:**
- Display strategy implementation (VNC bridge or alternative)
- SurfaceView rendering implementation
- Input event translation (touch, keyboard, mouse, gestures)
- IME (Input Method Editor) handling
- Performance optimization (frame rate, latency)

**Out of Scope:**
- VNC server setup (runs inside Ubuntu rootfs)
- Desktop environment configuration (part of rootfs)
- Android UI shell (handled by App Shell Agent)

### Key Responsibilities

1. **Display Pipeline Implementation**
   - Implement VNC client library (native or Java)
   - Connect VNC client to Android SurfaceView
   - Handle framebuffer updates and rendering
   - Optimize for 30+ FPS on mid-range devices

2. **Input Translation**
   - Touch events → Mouse pointer events
   - Multi-touch gestures → Mouse wheel, right-click, drag
   - Physical keyboard → X11 keysyms
   - Soft keyboard (IME) → X11 input
   - Special keys (Ctrl, Alt, Super, function keys)

3. **DesktopSurfaceView**
   - Custom SurfaceView for framebuffer rendering
   - Touch event interception and translation
   - Keyboard event handling
   - Zoom and pan support (optional)

4. **Performance Optimization**
   - Frame skipping under load
   - Differential framebuffer updates
   - Hardware-accelerated rendering (OpenGL/Vulkan)
   - Network bandwidth optimization

5. **Input Method Handling**
   - Show/hide soft keyboard on demand
   - Handle IME composition (for international input)
  - Clipboard integration (Android ↔ Ubuntu)

### Deliverables

- [ ] **VNC Client Implementation**
  - RFB protocol implementation (native library)
  - Connection management
  - Framebuffer decoding
  - Input event transmission

- [ ] **DesktopSurfaceView** (Kotlin)
  - SurfaceView subclass for rendering
  - Touch event handling
  - Keyboard event handling
  - OpenGL renderer (optional)

- [ ] **InputHandler** (Kotlin)
  - Touch → Pointer event translation
  - Gesture recognition (pinch, swipe, long-press)
  - Keyboard → Keysym translation
  - IME integration

- [ ] **VncBridgeService** (Kotlin)
  - Service for VNC client lifecycle
  - Connection to Ubuntu VNC server
  - Framebuffer callback to SurfaceView
  - Input event forwarding

- [ ] **Performance Monitor**
  - FPS counter
  - Latency measurement
  - Network usage tracking

- [ ] **Testing Suite**
  - Unit tests for input translation
  - Performance benchmarks
  - Mock VNC server for testing

### Sync Points

**Dependencies:**
- Requires display strategy decision from Requirements & Architecture Agent
- Requires DesktopActivity structure from Android App Shell Agent
- Requires session startup coordination from Linux/PRoot Integration Agent

**Coordinates With:**
- **Linux/PRoot Agent**: Ensure VNC server starts with session
- **Android App Shell Agent**: Integrate with DesktopActivity

**Blocks:**
- End-to-end user flows depend on GUI completion

---

## Agent 5: Packaging, Storage & Updates Agent

### Mission
Design and implement the rootfs packaging, distribution, storage, and update system, ensuring efficient downloads, integrity verification, and seamless updates.

### Scope

**In Scope:**
- Rootfs tarball hosting and distribution
- Download with resume and verification
- Storage layout and management
- Update mechanism (full or delta)
- Cache management
- Size optimization

**Out of Scope:**
- Android UI for download progress (provided by App Shell Agent)
- Rootfs extraction logic (handled by Linux/PRoot Integration Agent)

### Key Responsibilities

1. **Distribution Strategy**
   - Decide on APK bundling vs. on-demand download
   - Design APK split configuration (by architecture, by distro)
   - Define download URLs and versioning scheme
   - Implement CDN/hosting strategy

2. **Download System**
   - Resume-capable downloader
   - Progress reporting
   - Network condition handling (WiFi-only option)
   - Background download support (WorkManager)

3. **Integrity & Security**
   - SHA256 checksum verification
   - GPG signature verification (optional)
   - HTTPS for all downloads
   - Tamper detection

4. **Storage Management**
   - Efficient storage layout
   - Compression strategy (gzip, zstd)
   - Cache invalidation
   - Storage usage reporting
   - User-triggered cleanup

5. **Update Mechanism**
   - Version detection and notification
   - Background update downloads
   - Atomic update application
   - Rollback support
   - Delta updates (future)

### Deliverables

- [ ] **Distribution Design Document**
  - APK split strategy
  - Download URL structure
  - Versioning scheme
  - Hosting recommendations

- [ ] **RootfsDownloadService** (Kotlin)
  - Download with resume
  - Progress callbacks
  - Checksum verification
  - Error handling

- [ ] **StorageManager** (Kotlin)
  - Storage usage calculation
  - Cache management
  - Cleanup operations
  - Multi-distro layout

- [ ] **UpdateManager** (Kotlin)
  - Version check API
  - Update download orchestration
  - Atomic update application
  - Rollback mechanism

- [ ] **Compression Scripts**
  - Rootfs packaging script
  - Compression optimization
  - Integrity checksum generation

- [ ] **Server Infrastructure** (Optional)
  - Metadata API (versions, URLs, checksums)
  - CDN configuration
  - Update notification system

### Sync Points

**Dependencies:**
- Requires storage layout decisions from Requirements & Architecture Agent
- Requires download UI from Android App Shell Agent

**Coordinates With:**
- **Linux/PRoot Agent**: Define rootfs format and extraction requirements
- **Android App Shell Agent**: Integrate download progress UI

**Blocks:**
- Setup wizard depends on download service
- Update feature depends on update manager

---

## Agent 6: Developer Experience & Automation Agent

### Mission
Create the tooling, scripts, and documentation that enable efficient development, testing, building, and releasing of the Ubuntu-on-Android application.

### Scope

**In Scope:**
- Build system configuration (Gradle, CMake, NDK)
- CI/CD pipeline setup
- Local development tooling
- Testing infrastructure
- Documentation for contributors
- Release automation

**Out of Scope:**
- Application feature implementation
- Native library implementation (build setup only)

### Key Responsibilities

1. **Build System**
   - Gradle configuration for app and native libraries
   - CMake setup for native components
   - APK split configuration
   - Signing configuration
   - Build variants (debug, release, staging)

2. **CI/CD Pipeline**
   - GitHub Actions / GitLab CI configuration
   - Automated builds on commit
   - Automated testing (unit, integration)
   - APK artifact generation
   - Release automation (tag → APK → GitHub release)

3. **Local Development Tools**
   - Quick-run scripts for development
   - Mock rootfs generator for UI testing
   - Hot-reload configuration
   - Debug logging setup
   - ADB helper scripts

4. **Testing Infrastructure**
   - Unit test setup (JUnit, MockK)
   - Instrumentation test setup (AndroidX Test)
   - Mock native library for testing
   - Test data fixtures
   - Coverage reporting

5. **Documentation**
   - Contributor guide
   - Build instructions
   - Architecture overview
   - API documentation
   - Troubleshooting guide

### Deliverables

- [ ] **Build Configuration**
  - `build.gradle` files for app and modules
  - `CMakeLists.txt` for native libraries
  - `gradle.properties` for signing and versions
  - ProGuard/R8 rules

- [ ] **CI/CD Pipeline**
  - GitHub Actions workflow
  - Docker container for build environment
  - Automated release workflow
  - Status badges

- [ ] **Development Scripts**
  - `./gradlew assembleDebug` wrapper
  - `scripts/install-device.sh` (ADB install)
  - `scripts/mock-rootfs.sh` (generate test data)
  - `scripts/run-emulator.sh` (launch emulator)

- [ ] **Testing Setup**
  - JUnit configuration
  - MockK dependencies
  - Espresso configuration
  - Test coverage setup (JaCoCo)

- [ ] **Documentation**
  - `README.md` with quick start
  - `CONTRIBUTING.md` for contributors
  - `docs/ARCHITECTURE.md` (already exists)
  - `docs/BUILD.md` for build instructions
  - `docs/TESTING.md` for testing guide

- [ ] **Release Automation**
  - Version bump script
  - Changelog generator
  - APK signing script
  - GitHub release creation script

### Sync Points

**Dependencies:**
- Requires project structure from all other agents
- Requires native library sources from Linux/PRoot and GUI agents

**Coordinates With:**
- All agents for build integration

**Enables:**
- Efficient development for all teams
- Automated testing and releases

---

## Coordination Protocol

### Communication Channels

1. **Architecture Document**: Single source of truth for system design
2. **Interface Specifications**: API contracts defined before implementation
3. **Sync Point Meetings**: Virtual checkpoints when dependencies require coordination

### Decision Process

1. **Proposed Change**: Agent documents proposed change in relevant spec
2. **Impact Analysis**: Identify dependent agents/components
3. **Review**: Affected agents review and provide feedback
4. **Approval**: Architecture agent approves if no conflicts
5. **Update**: Update all relevant specifications
6. **Migration Plan**: If breaking change, provide migration path for dependent agents

### Conflict Resolution

- **Technical Disagreement**: Requirements & Architecture Agent makes final call
- **Priority Conflict**: Project lead (Scaffolding Orchestrator) prioritizes
- **Blocking Issue**: Escalate to project lead with impact analysis

### Definition of Done

Each agent task is complete when:
- [ ] Code implemented and tested
- [ ] Documentation updated
- [ ] Interface contracts honored
- [ ] Integration points verified
- [ ] No known regressions in dependent components

---

# Codebase Snapshot (Authoritative)

This section is intentionally narrow: it documents what exists in this repository today and the conventions you should follow when modifying it.

## Primary entry points

- `app/src/main/java/com/udroid/app/MainActivity.kt`
  - Permissions gate (`REQUIRED_PERMISSIONS`)
  - Compose navigation (`NavHost`) routes:
    - `permissions`
    - `session_list`
    - `setup_wizard`
    - `desktop/{sessionId}`

- `app/src/main/java/com/udroid/app/service/UbuntuSessionService.kt`
  - Foreground service actions:
    - `ACTION_START_SESSION`
    - `ACTION_STOP_SESSION`
  - Uses injected `UbuntuSessionManager` (Hilt)

## Session management (contracts you must preserve)

- `app/src/main/java/com/udroid/app/session/UbuntuSession.kt`
  - `UbuntuSessionManager` is the public API used by UI + service
  - `UbuntuSession` exposes:
    - `state: SessionState`
    - `stateFlow: Flow<SessionState>`
    - `start()` / `stop()` / `exec(command)` as `Result`-returning suspend functions

- `app/src/main/java/com/udroid/app/session/UbuntuSessionManagerImpl.kt`
  - `UbuntuSessionManagerImpl` is the current implementation
  - `UbuntuSessionImpl` is currently defined in this same file
  - `UbuntuSessionImpl.start()` and `exec()` are stubs today; they still must:
    - Update persisted session state via `SessionRepository.updateSessionState()`
    - Return `Result.failure(...)` on invalid lifecycle transitions

## Persistence model (single source of truth)

- `app/src/main/java/com/udroid/app/storage/SessionRepository.kt`
  - Uses DataStore Preferences
  - Persists `SessionInfo` as JSON (`kotlinx.serialization`)
  - Persists a set of IDs under `session_ids` as a comma-separated string
  - Session state is persisted as `SessionStateData`
  - Domain/data conversions are authoritative:
    - `fun SessionStateData.toDomain(): SessionState`
    - `fun SessionState.toData(): SessionStateData`

## Domain model

- `app/src/main/java/com/udroid/app/model/DistroVariant.kt`
  - `DistroVariant` carries the distro id used for persistence and interop
  - `SessionState` and `ProcessResult` live here

## UI layer

- `app/src/main/java/com/udroid/app/ui/setup/SetupWizardScreen.kt`
- `app/src/main/java/com/udroid/app/ui/setup/SetupWizardViewModel.kt`
- `app/src/main/java/com/udroid/app/ui/session/SessionListScreen.kt`
- `app/src/main/java/com/udroid/app/ui/session/SessionListViewModel.kt`
- `app/src/main/java/com/udroid/app/ui/desktop/DesktopScreen.kt` (placeholder rendering)

UI conventions:

- UI does not own state; ViewModels expose `StateFlow`
- UI collects flows with `collectAsState()`
- ViewModels call manager methods and handle `Result.fold(...)` with Timber logging

## Native bridge

- `app/src/main/java/com/udroid/app/nativebridge/NativeBridge.kt`
  - Loads `udroid-native` if available
  - Provides stubbed methods for launching/killing proot processes
  - Any future JNI implementation must preserve method signatures or provide a migration plan

## Conventions (keep context pristine)

- **No invented architecture**: if a component is not present in this repo, describe it as planned work and keep it out of the “authoritative” sections.
- **Prefer referencing file paths over paraphrasing** when documenting behavior.
- **State writes**: if you change session lifecycle, ensure state transitions are persisted via `SessionRepository.updateSessionState()`.
- **Error handling**: use `Result` and avoid throwing across UI/service boundaries.
- **Logging**: use `Timber` consistently.

