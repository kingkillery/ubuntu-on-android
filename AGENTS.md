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
- Android app architecture (Activities, Services, Fragments)
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
   - Implement Activity/Fragment structure
   - Design navigation graph
   - Setup dependency injection (Koin/Hilt)
   - Define ViewModel layer for session state

2. **Permission Management**
   - Request storage, network, foreground service permissions
   - Handle permission denial gracefully
   - Show explanatory UI for permission rationale

3. **Session Management UI**
   - Session list screen (create, delete, start, stop sessions)
   - Session details screen (logs, stats, configuration)
   - Session creation wizard (distro selection, configuration)
   - Desktop activity (full-screen session view)

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

- [ ] **MainActivity**
  - Permission request flow
  - Navigation to setup wizard or session list
  - App initialization checks

- [ ] **SetupWizardActivity**
  - First-run setup flow
  - Distro selection screen
  - Rootfs download initiation
  - Progress display

- [ ] **SessionListActivity**
  - RecyclerView of sessions
  - FAB for creating new session
  - Swipe-to-delete functionality
  - Session status indicators

- [ ] **DesktopActivity**
  - Full-screen SurfaceView for desktop rendering
  - Status bar with session info
  - Keyboard handling
  - Input method handling

- [ ] **SettingsFragment**
  - App settings preferences
  - Session-specific settings
  - Storage usage display
  - About and help

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
- GUI implementation depends on DesktopActivity structure
- Testing depends on service and ViewModel completion

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

# Implementation Architecture Notes

## Phase 1: Foundation (COMPLETED ✅)

### Implemented Components

#### 1. Core Architecture
**File:** `session/UbuntuSession.kt`
- Interface definitions for session management
- `UbuntuSessionManager` - CRUD operations for sessions
- `UbuntuSession` - Individual session lifecycle
- `SessionConfig` - Configuration data class

**Design Decisions:**
- Interfaces first: Define contracts before implementation
- Coroutines + Flow: Reactive, async operations
- Result types: Explicit error handling with `Result<T>`

#### 2. Data Persistence Layer
**File:** `storage/SessionRepository.kt`
- DataStore for persistent storage
- Serialization with kotlinx.serialization
- Flow-based reactive data streams

**Architecture Pattern:**
```
UI (Compose) 
  ↓ observes Flow
ViewModel
  ↓ calls suspend functions
Repository (DataStore)
  ↓ serializes/deserializes
Disk (DataStore prefs)
```

**Key Design:**
- `SessionInfo` - Persistable data model
- `SessionStateData` - Serializable state wrapper
- Extension functions for domain ↔ data conversion
- IDs stored as comma-separated string for O(1) lookup

#### 3. Native Bridge (Stub)
**File:** `native/NativeBridge.kt`
- JNI interface placeholder
- Stub implementations for all native operations
- Graceful fallback when native lib unavailable

**Future Native Functions:**
```kotlin
external fun launchProot(...)
external fun waitForProot(...)
external fun killProot(...)
external fun getProotStdout(...)
external fun getProotStderr(...)
```

#### 4. Session Manager Implementation
**File:** `session/UbuntuSessionManagerImpl.kt`
- Full CRUD operations for sessions
- State synchronization with repository
- Process lifecycle management

**Session Lifecycle:**
```
Created → Starting → Running (vncPort) → Stopping → Stopped
                    ↓
                  Error (message)
```

**Key Features:**
- UUID-based session IDs
- Automatic state persistence on changes
- Cleanup on session delete (stop if running)

#### 5. Foreground Service
**File:** `service/UbuntuSessionService.kt`
- Foreground service for session management
- Notification with stop action
- Binder interface for activity communication

**Notification Architecture:**
```
Service creates notification
  ↓
User can tap notification → Open MainActivity
User can tap Stop → Stop session via Service
```

#### 6. UI Layer (Jetpack Compose)

##### Setup Wizard
**Files:** 
- `ui/setup/SetupWizardViewModel.kt`
- `ui/setup/SetupWizardScreen.kt`

**Flow:**
1. User selects distro (4 options)
2. User enters session name
3. Click "Create Session"
4. ViewModel calls SessionManager.createSession()
5. On success, navigate back to session list

##### Session List
**Files:**
- `ui/session/SessionListViewModel.kt`
- `ui/session/SessionListScreen.kt`

**Features:**
- RecyclerView of all sessions
- FAB to create new session
- Start/Stop buttons per session
- Delete with confirmation dialog
- Empty state when no sessions
- State badges (Created, Starting, Running, etc.)

##### Navigation
**File:** `MainActivity.kt` - Updated with NavHost

**Navigation Graph:**
```
permissions → session_list
                   ↓
              setup_wizard
```

**Pattern:** Navigation as state, simple composable destinations

### Technology Stack Summary

**Framework:** Jetpack Compose (no XML layouts)
**DI:** Hilt (compile-time dependency injection)
**Async:** Kotlin Coroutines + Flow
**Persistence:** DataStore Preferences (not SharedPreferences)
**Serialization:** kotlinx.serialization (JSON)
**Logging:** Timber
**Navigation:** Compose Navigation
**Theme:** Material 3 with Ubuntu brand colors

---

## Architecture Patterns Used

### 1. Repository Pattern
```kotlin
// Single source of truth for session data
interface SessionRepository {
    suspend fun saveSession(session: SessionInfo)
    fun observeSessions(): Flow<List<SessionInfo>>
}
```

### 2. ViewModel Pattern
```kotlin
@HiltViewModel
class SessionListViewModel @Inject constructor(
    private val sessionManager: UbuntuSessionManager
) : ViewModel()
```

### 3. Dependency Injection
```kotlin
@HiltAndroidApp
class UdroidApplication : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity()
```

### 4. State Management
```kotlin
// Single source of truth in ViewModel
private val _sessions = MutableStateFlow<List<Session>>(emptyList())
val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

// UI observes
val sessions by viewModel.sessions.collectAsState()
```

### 5. Result Types
```kotlin
// Explicit error handling
suspend fun createSession(config: SessionConfig): Result<UbuntuSession>

// Usage
result.fold(
    onSuccess = { session -> /* handle success */ },
    onFailure = { error -> /* handle error */ }
)
```

---

## File Organization

```
app/src/main/java/com/udroid/app/
├── model/                    # Domain models (sealed classes, enums)
│   └── DistroVariant.kt
├── session/                  # Business logic (interfaces + impl)
│   ├── UbuntuSession.kt      # Interfaces
│   └── UbuntuSessionManagerImpl.kt
├── storage/                  # Data persistence
│   └── SessionRepository.kt
├── native/                   # JNI bridge
│   └── NativeBridge.kt
├── service/                  # Android services
│   └── UbuntuSessionService.kt
└── ui/                       # Compose UI
    ├── theme/                # Material 3 theming
    ├── setup/                # Setup wizard
    │   ├── SetupWizardViewModel.kt
    │   └── SetupWizardScreen.kt
    └── session/              # Session management
        ├── SessionListViewModel.kt
        └── SessionListScreen.kt
```

---

## Data Flow Diagrams

### Creating a Session
```
User clicks "Create" in SetupWizardScreen
  ↓
SetupWizardViewModel.createSession()
  ↓
UbuntuSessionManager.createSession(config)
  ↓
SessionRepository.saveSession(info)
  ↓
DataStore persists to disk
  ↓
_setupComplete.value = sessionId
  ↓
LaunchedEffect navigates to SessionListScreen
  ↓
SessionListViewModel observes new session via Flow
  ↓
UI recomposes with new session in list
```

### Starting a Session
```
User clicks Play button in SessionListScreen
  ↓
SessionListViewModel.startSession(sessionId)
  ↓
UbuntuSessionManager.startSession(sessionId)
  ↓
UbuntuSessionImpl.start()
  ↓
State: Created → Starting
  ↓
SessionRepository.updateSessionState()
  ↓
NativeBridge.launchProot() [STUB - will be JNI call]
  ↓
State: Starting → Running(vncPort)
  ↓
SessionRepository.updateSessionState()
  ↓
Flow emits new state to all observers
  ↓
UI recomposes with "Running" badge
```

---

## Key Design Decisions

### 1. DataStore over SharedPreferences
**Why:**
- Coroutines-based (no blocking main thread)
- Type-safe with Flow
- Transactional updates
- Migration path to Room database if needed

### 2. Flow over LiveData
**Why:**
- Part of Kotlin coroutines ecosystem
- Transformations operators (map, filter)
- Not tied to Android lifecycle
- Works with DataStore out of the box

### 3. Compose over XML
**Why:**
- Declarative UI
- Less boilerplate
- State management built-in
- Modern Android best practice
- Preview tooling

### 4. Hilt over Koin
**Why:**
- Compile-time validation
- Better performance (no reflection)
- Google recommended
- ViewModel injection support
- Works with Compose Navigation

### 5. Single Activity Architecture
**Why:**
- Navigation Compose handles back stack
- Easier state management
- Performance (single process)
- Modern Android pattern

### 6. Interface-First Design
**Why:**
- Easy testing (can mock interfaces)
- Parallel development (define contracts)
- Flexibility (swap implementations)
- Clear boundaries between layers

---

## Testing Strategy

### Unit Tests (Not Yet Implemented)
```kotlin
class SessionListViewModelTest {
    @Test
    fun `deleteSession should call sessionManager delete`() = runTest {
        // Given
        val mockSessionManager = mockk<UbuntuSessionManager>()
        val viewModel = SessionListViewModel(mockSessionManager)
        
        // When
        viewModel.deleteSession("session-id")
        
        // Then
        coVerify { mockSessionManager.deleteSession("session-id") }
    }
}
```

### Integration Tests (Not Yet Implemented)
```kotlin
@RunWith(AndroidJUnit4::class)
class SessionListScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `clicking FAB navigates to setup wizard`() {
        composeTestRule.setContent {
            SessionListScreen(
                onCreateSession = { /* verify */ },
                onSessionClick = {}
            )
        }
        
        composeTestRule
            .onNodeWithContentDescription("Create Session")
            .performClick()
        
        // Verify navigation
    }
}
```

---

## Known Limitations

### Current Stub Implementations
1. **NativeBridge** - No actual PRoot execution
   - TODO: Implement JNI bridge
   - TODO: Port PRoot to Android

2. **Rootfs Download** - Not implemented
   - TODO: Create RootfsDownloadService
   - TODO: Add download UI to SetupWizard

3. **Desktop Surface** - Not implemented
   - TODO: Create DesktopActivity with SurfaceView
   - TODO: Implement VNC client

4. **VNC Server** - Not started
   - TODO: Integrate VNC server startup in session launch
   - TODO: Implement VncBridgeService

### Session Persistence
- Data stored in DataStore Preferences
- Not encrypted (future: use DataStore Proto or EncryptedSharedPreferences)
- No backup/restore (future: integrate with Android auto-backup)

### State Synchronization
- No conflict resolution (single-user app)
- No multi-device sync (by design)

---

## Next Implementation Phases

### Phase 2: Core Services (In Progress)
- [ ] Implement actual PRoot execution
- [ ] Add rootfs download UI
- [ ] Create DesktopActivity stub
- [ ] Implement VNC bridge stub

### Phase 3: Native Integration
- [ ] Port PRoot to Android
- [ ] Create JNI bridge
- [ ] Implement VNC client

### Phase 4: Polish
- [ ] Error handling improvements
- [ ] Loading states
- [ ] Progress indicators
- [ ] Unit tests
- [ ] Documentation

---

## Dependencies Added

### Core Android
- androidx.core:core-ktx:1.12.0
- androidx.lifecycle:lifecycle-*:2.7.0
- androidx.activity:activity-compose:1.8.1

### Compose
- androidx.compose.*:compose-bom:2023.10.01
- androidx.compose.material3:material3

### Navigation
- androidx.navigation:navigation-compose:2.7.5

### Dependency Injection
- com.google.dagger:hilt-android:2.48
- androidx.hilt:hilt-navigation-compose:1.1.0

### Async
- org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3

### Persistence
- androidx.datastore:datastore-preferences:1.0.0
- org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0

### Work Manager
- androidx.work:work-runtime-ktx:2.9.0

### Logging
- com.jakewharton.timber:timber:5.0.1

### Testing
- junit:junit:4.13.2
- org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3
- io.mockk:mockk:1.13.8
- androidx.test.ext:junit:1.1.5
- androidx.test.espresso:espresso-core:3.5.1

---

## Build Configuration

### Gradle Versions
- Gradle: 8.2
- Android Gradle Plugin: 8.2.0
- Kotlin: 1.9.20
- Hilt: 2.48
- KSP: 1.9.20-1.0.14

### App Configuration
- compileSdk: 34
- minSdk: 26
- targetSdk: 34
- namespace: com.udroid.app

### ABIs Supported
- arm64-v8a (64-bit ARM - most modern phones)
- armeabi-v7a (32-bit ARM - older phones)
- x86_64 (64-bit x86 - emulators, Intel devices)

### Build Variants
- debug: .debug suffix, debuggable
- release: minify + shrinkResources, ProGuard
- staging: .staging suffix, minified

---

## Performance Considerations

### Optimizations Applied
1. **LazyColumn** instead of Column for lists (lazy loading)
2. **derivedStateOf** not needed yet (no complex computations)
3. **remember** for expensive computations
4. **Flow** vs **LiveData** - Flow is more flexible

### Future Optimizations
- [ ] Paging library for large session lists
- [ ] LazyRow/Grid for distro selection
- [ ] Image loading caching for distro thumbnails
- [ ] Database indexes if migrating to Room
- [ ] Background coroutine dispatchers (IO vs Default)

---

## Accessibility

### Current Support
- Content descriptions on buttons
- Semantic headings (titleLarge, headlineMedium)
- Sufficient color contrast (Material 3 default)

### TODO
- [ ] Screen reader announcements for state changes
- [ ] Touch target size (48dp minimum)
- [ ] Keyboard navigation support
- [ ] High contrast mode
- [ ] Font scaling support

---

## Security Considerations

### Current State
- Permissions requested at runtime
- No sensitive data in logs
- HTTPS only for network (usesCleartextTraffic=false)

### TODO
- [ ] Encrypt session data at rest
- [ ] Certificate pinning for downloads
- [ ] Rootfs signature verification
- [ ] Sandbox isolation for PRoot
- [ ] SELinux policies

---

## Monitoring & Analytics

### Current State
- Timber logging for development
- No crash reporting
- No analytics

### TODO
- [ ] Add Firebase Crashlytics
- [ ] Add analytics for:
  - Session creation rate
  - Session success/failure rate
  - Most popular distros
  - Device performance metrics
- [ ] Performance monitoring
- [ ] Crash-free users metric

---

## Localization

### Current State
- All strings hardcoded in English
- No resource qualifiers for other languages

### TODO
- [ ] Extract all strings to strings.xml
- [ ] Add translations (Spanish, Chinese, etc.)
- [ ] RTL layout support
- [ ] Locale-aware formatting (dates, numbers)

---

## Offline Support

### Current State
- No offline functionality
- Session list persists locally (DataStore)
- No network calls yet

### TODO
- [ ] Cache rootfs metadata
- [ ] Download resume support
- [ ] Offline session creation (if rootfs downloaded)
- [ ] Network status indicator

---

## Backward Compatibility

### Strategy
- Min SDK 26 (Android 8.0) - 98%+ coverage
- No reflection (Hilt compile-time)
- No deprecated APIs
- Forward-compatible with Android 14+

### Migration Path
- DataStore provides built-in migration
- Room can replace DataStore if needed
- Backup/restore via Android auto-backup

---

## Debugging

### Tools
- Timber for logging
- Hilt dependency graph verification
- Layout Inspector for Compose
- Network Inspector (for future downloads)
- Database Inspector (for DataStore/Rom)

### Common Issues
1. **Hilt not injecting** - Check @HiltAndroidApp, @AndroidEntryPoint
2. **Navigation not working** - Verify NavHost setup, startDestination
3. **Flow not emitting** - Ensure collectAsState() in Composable
4. **Permissions** - Check manifest + runtime requests

---

## Performance Profiling

### Tools to Use
- Android Studio Profiler (CPU, Memory, Network)
- Compose Compiler metrics
- Macrobenchmark (future)
- Baseline Profiles (future)

### Metrics to Track
- App startup time (< 3 seconds)
- Session creation time (< 5 seconds)
- Session startup time (< 10 seconds)
- Memory usage (< 200MB baseline)
- Battery usage (optimize later)

---

## Code Quality

### Standards
- Kotlin coding conventions
- Compose best practices
- No hardcoded dimensions (use dimens.xml)
- No magic numbers (extract to constants)
- Meaningful variable names
- KDoc comments for public APIs

### Lint Checks
- Android Lint enabled
- Detekt (static analysis) - TODO
-ktlint (formatting) - TODO
- Unused resources check

---

## Documentation

### Completed
- [x] ARCHITECTURE.md - System design
- [x] AGENTS.md - Agent charters + THIS SECTION
- [x] TASKS.md - Task breakdown
- [x] SCAFFOLD.md - Project scaffold
- [x] PROGRESS.md - Implementation status
- [x] README.md - Project overview

### TODO
- [ ] API documentation (KDoc)
- [ ] Developer guide
- [ ] User guide
- [ ] Troubleshooting guide
- [ ] Architecture decision records (ADRs)

---

## Deployment

### Distribution Channels
- GitHub Releases (open source)
- Google Play Store (future - needs account)
- F-Droid (future - needs publishing)
- Direct APK download (from releases)

### Signing
- Debug: Default debug keystore
- Release: Release keystore (not committed)
- APK splits by ABI for smaller downloads

### Versioning
- Semantic versioning (MAJOR.MINOR.PATCH)
- Build number increments with each build
- Version name displayed in settings

---

## Contributing

### Getting Started
1. Clone repository
2. Open in Android Studio
3. Sync Gradle
4. Run on device/emulator
5. Pick a task from PROGRESS.md

### Coding Standards
- Follow Kotlin style guide
- Write unit tests for ViewModels
- Update documentation for API changes
- Run lint before committing

### Pull Request Process
1. Fork repository
2. Create feature branch
3. Make changes
4. Add tests
5. Update documentation
6. Submit PR with description

---

## Release Notes

### v0.1.0 (Current - Development)
**Features:**
- Session management UI
- Create/delete/start/stop sessions
- 4 Ubuntu distro options (Jammy XFCE4, MATE, GNOME, Noble)
- Persistent session storage
- Permission handling
- Material 3 theming

**Limitations:**
- No actual Ubuntu execution (stub)
- No rootfs download
- No desktop display
- Testing not implemented

**Next Release (v0.2.0):**
- Implement actual PRoot execution
- Add rootfs download
- Create desktop surface
- Add unit tests

---
