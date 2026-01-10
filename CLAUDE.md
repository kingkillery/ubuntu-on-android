# CLAUDE.md — Working Agreement for This Repo

This file is the **operational contract** for using Claude on this repository. It is intentionally concrete and file-path-driven.

## 1) Prime directive: keep context pristine

- **Do not invent architecture.** If it is not present in the repository, treat it as *planned* and do not describe it as implemented.
- **Cite file paths.** When you claim “X does Y”, include the authoritative file path(s) that implement that behavior.
- **Prefer small, local edits** over broad refactors unless explicitly requested.
- **Preserve public contracts** (interfaces + persisted schema). If you must change them, explain migrations and update all call sites.

## 2) Where truth lives (authoritative code touchpoints)

### App entry points

- `app/src/main/java/com/udroid/app/UdroidApplication.kt`
  - Hilt app (`@HiltAndroidApp`)
  - Timber setup

- `app/src/main/java/com/udroid/app/MainActivity.kt`
  - Permission gate (`REQUIRED_PERMISSIONS`)
  - Compose Navigation routes:
    - `permissions`
    - `session_list`
    - `setup_wizard`
    - `desktop/{sessionId}`

- `app/src/main/java/com/udroid/app/service/UbuntuSessionService.kt`
  - Foreground service action API:
    - `ACTION_START_SESSION`
    - `ACTION_STOP_SESSION`
  - Injects `UbuntuSessionManager`

- `app/src/main/java/com/udroid/app/service/RootfsDownloadService.kt`
  - Foreground download service (OkHttp download + notification updates)
  - Rootfs extraction is delegated to `RootfsManager` (currently stubbed)

### Session layer (public contracts)

- `app/src/main/java/com/udroid/app/session/UbuntuSession.kt`
  - `UbuntuSessionManager` API used by UI + service
  - `UbuntuSession` lifecycle contract:
    - `state: SessionState`
    - `stateFlow: Flow<SessionState>`
    - `start()` / `stop()` / `exec(command)` return `Result`

- `app/src/main/java/com/udroid/app/session/UbuntuSessionManagerImpl.kt`
  - `UbuntuSessionManagerImpl` is the DI implementation
  - `UbuntuSessionImpl` currently lives in this same file

### Persistence (single source of truth)

- `app/src/main/java/com/udroid/app/storage/SessionRepository.kt`
  - DataStore Preferences
  - Persisted models:
    - `SessionInfo` (JSON)
    - `SessionStateData` (sealed, serializable)
  - Canonical conversion functions:
    - `fun SessionStateData.toDomain(): SessionState`
    - `fun SessionState.toData(): SessionStateData`

### Domain model

- `app/src/main/java/com/udroid/app/model/DistroVariant.kt`
  - `DistroVariant` (id, display name, size, desktop)
  - `SessionState` (domain) + `ProcessResult`

### UI layer

- `app/src/main/java/com/udroid/app/ui/setup/SetupWizardScreen.kt`
- `app/src/main/java/com/udroid/app/ui/setup/SetupWizardViewModel.kt`
- `app/src/main/java/com/udroid/app/ui/session/SessionListScreen.kt`
- `app/src/main/java/com/udroid/app/ui/session/SessionListViewModel.kt`
- `app/src/main/java/com/udroid/app/ui/desktop/DesktopScreen.kt` (placeholder rendering surface)

### Native bridge

- `app/src/main/java/com/udroid/app/nativebridge/NativeBridge.kt`
  - Loads `udroid-native` if available
  - Provides stub methods for launching/killing proot processes

## 3) Invariants you must not break

- **Session state persistence**
  - Any lifecycle transition must be persisted via `SessionRepository.updateSessionState(sessionId, ...)`.
  - If you add a new state, you must update:
    - `SessionState` (domain)
    - `SessionStateData` (persisted)
    - `toData()` / `toDomain()` conversions

- **Result-based error flow**
  - Session APIs return `Result` and UI uses `Result.fold(...)`.
  - Avoid throwing exceptions across UI/service boundaries.

- **Flow-first observation**
  - UI should observe via `StateFlow`/`Flow` and collect with `collectAsState()`.

- **Distro identity stability**
  - `DistroVariant.id` is persisted (`SessionInfo.distroId`). Treat ids as stable external identifiers.

## 4) Coding conventions (as practiced in this repo)

- **Kotlin + Coroutines + Flow**: suspend functions for work, flows for observation.
- **Compose**: stateless composables; ViewModels own mutable state.
- **Hilt**: inject into ViewModels/Services; don’t manual-singleton things.
- **Logging**: use `Timber`.

## 5) What “done” looks like for a change

- **Correctness**: all call sites updated, compilation succeeds.
- **Contracts preserved**: session lifecycle + persistence conversions updated if applicable.
- **No speculation in docs**: documentation only states what’s in-tree.

## 6) Typical edit checklists

### If you touch session lifecycle

- Update `UbuntuSessionImpl` state transitions.
- Ensure persisted state is updated through `SessionRepository.updateSessionState()`.
- Ensure UI badges still map to the correct states (`SessionListScreen.kt`).

### If you touch persistence schema

- Update both `SessionInfo` and any related conversion helpers.
- Update `Json { ignoreUnknownKeys = true; encodeDefaults = true }` assumptions carefully.
- Consider forward/backward compatibility of stored values.

### If you add a new UI screen

- Add a new route in `MainActivity.kt` NavHost.
- Add a ViewModel if it owns state; keep composables mostly stateless.

