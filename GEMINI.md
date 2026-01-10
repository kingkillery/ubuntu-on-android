# GEMINI.md — Repo-Specific Guidance

This file defines how Gemini should operate in this repository: **accurate, path-cited, and contract-preserving**.

## 1) Accuracy rules (non-negotiable)

- **No hallucinations**: do not claim a class/module exists unless you can point at its file path.
- **Authoritative vs planned**: keep implemented behavior separate from future plans.
- **Use the repo’s language**: Kotlin, Coroutines/Flow, Hilt, Compose.

## 2) Key architecture (implemented today)

### Navigation + permissions

- `app/src/main/java/com/udroid/app/MainActivity.kt`
  - `REQUIRED_PERMISSIONS` is the current permissions gate.
  - Routes are:
    - `permissions`
    - `session_list`
    - `setup_wizard`
    - `desktop/{sessionId}`

### Session API

- `app/src/main/java/com/udroid/app/session/UbuntuSession.kt`
  - `UbuntuSessionManager` is the boundary used by UI + service.
  - All lifecycle calls return `Result`.

### Session implementation

- `app/src/main/java/com/udroid/app/session/UbuntuSessionManagerImpl.kt`
  - `UbuntuSessionManagerImpl` provides CRUD + start/stop.
  - `UbuntuSessionImpl` is currently in the same file.
  - Startup/exec are stubs, but state transitions + persistence are real concerns.

### Persistence

- `app/src/main/java/com/udroid/app/storage/SessionRepository.kt`
  - DataStore Preferences is the persistence layer.
  - Session state is stored as `SessionStateData` and converted to `SessionState` via:
    - `SessionStateData.toDomain()`
    - `SessionState.toData()`

### Native bridge

- `app/src/main/java/com/udroid/app/nativebridge/NativeBridge.kt`
  - Loads `udroid-native` if present.
  - Provides stub methods for proot process control.

## 3) Conventions to follow

- **State ownership**:
  - ViewModels own mutable state (`MutableStateFlow`).
  - UI collects via `collectAsState()`.

- **Error propagation**:
  - Prefer `Result.failure(...)` and `Result.fold(...)`.
  - Avoid exceptions crossing module boundaries.

- **Persistence discipline**:
  - If state changes, persist it (`SessionRepository.updateSessionState`).
  - If schema changes, update conversions and keep compatibility in mind.

- **Logging**:
  - Use `Timber`.

## 4) Safe change patterns

- **When changing session states**:
  - Update both `SessionState` (domain) and `SessionStateData` (persisted).
  - Update `toDomain()` and `toData()`.
  - Confirm UI badges/logic handle the new states.

- **When adding screens/routes**:
  - Add the route to the `NavHost` in `MainActivity.kt`.
  - Keep composables stateless and push logic into ViewModels.

## 5) What not to do

- Don’t describe non-existent components as implemented (e.g., “DesktopActivity” if it doesn’t exist).
- Don’t reformat/rename large code areas unless asked.
- Don’t introduce new frameworks/dependencies just for style or preference.

