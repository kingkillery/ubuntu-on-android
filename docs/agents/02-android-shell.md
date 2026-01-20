# Agent 2: Android App Shell

## Mission

Design and implement the Android application framework that manages Ubuntu sessions, handles permissions, provides UI, and exposes integration points for the native Ubuntu environment.

## Scope

**In Scope:**
- Android app architecture (single-activity Compose + foreground service)
- Permission request flows
- Session management UI
- Settings and configuration UI
- Foreground service for background session management
- Integration hooks for native components

**Out of Scope:**
- Native library implementation (JNI interfaces only)
- Desktop rendering implementation (SurfaceView stubs only)
- Rootfs download implementation (service stubs only)

## Key Responsibilities

### App Architecture
- Maintain single-activity Compose architecture (`MainActivity` + `NavHost`)
- Navigation routes: `permissions`, `session_list`, `setup_wizard`, `desktop/{sessionId}`, `terminal/{sessionId}`, `agent_task/{sessionId}`
- Hilt dependency injection for ViewModels and services
- UI observes `Flow`/`StateFlow` without manual lifecycle plumbing

### Permission Management
- Request storage, network, foreground service permissions
- Handle permission denial gracefully
- Show explanatory UI for permission rationale

### Session Management UI
- Session list: create/delete/start/stop sessions
- Setup wizard: distro selection + session name + create
- Desktop screen: placeholder surface, stop control, state display
- Terminal screen: command input/output
- Agent task screen: agent tool execution

### Background Services
- `UbuntuSessionService` for foreground session management
- Notification channel for session status
- Service lifecycle management

## Authoritative Files

```
app/src/main/java/com/udroid/app/MainActivity.kt
app/src/main/java/com/udroid/app/ui/session/SessionListScreen.kt
app/src/main/java/com/udroid/app/ui/session/SessionListViewModel.kt
app/src/main/java/com/udroid/app/ui/setup/SetupWizardScreen.kt
app/src/main/java/com/udroid/app/ui/setup/SetupWizardViewModel.kt
app/src/main/java/com/udroid/app/ui/desktop/DesktopScreen.kt
app/src/main/java/com/udroid/app/ui/terminal/TerminalScreen.kt
app/src/main/java/com/udroid/app/ui/agent/AgentTaskScreen.kt
app/src/main/java/com/udroid/app/service/UbuntuSessionService.kt
```

## Sync Points

**Dependencies:**
- Requires architecture decisions from Agent 1
- Requires interface definitions for `UbuntuSessionManager`
- Coordinates with Agent 3 for native bridge calls

**Blocks:**
- GUI implementation depends on desktop rendering pipeline
