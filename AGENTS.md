# AGENTS.md - Ubuntu on Android

Native Android app running Ubuntu desktop via PRoot in an app sandbox.

## Quick Reference

| Item | Value |
|------|-------|
| **Package** | `com.udroid.app` |
| **Build** | `./gradlew assembleDebug` |
| **Stack** | Kotlin, Jetpack Compose, Hilt, Coroutines/Flow |
| **Min SDK** | 24 (Android 7.0) |

## Architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md) for:
- Component breakdown and data flow
- Session management contracts (`UbuntuSession`, `UbuntuSessionManager`)
- Storage layout and filesystem paths
- Display pipeline strategy

## Agent Charters

Each agent has a defined scope, responsibilities, and deliverables:

| Agent | Focus Area | Charter |
|-------|------------|---------|
| Requirements & Architecture | PRD, system design, API contracts | [docs/agents/01-architecture.md](./docs/agents/01-architecture.md) |
| Android App Shell | UI, permissions, services, navigation | [docs/agents/02-android-shell.md](./docs/agents/02-android-shell.md) |
| Linux/PRoot Integration | PRoot, rootfs, session launch, JNI | [docs/agents/03-proot-integration.md](./docs/agents/03-proot-integration.md) |
| GUI & Interaction | Display pipeline, input, VNC bridge | [docs/agents/04-gui-interaction.md](./docs/agents/04-gui-interaction.md) |
| Packaging & Storage | Downloads, updates, storage mgmt | [docs/agents/05-packaging-storage.md](./docs/agents/05-packaging-storage.md) |
| Developer Experience | Build, CI/CD, testing, docs | [docs/agents/06-developer-experience.md](./docs/agents/06-developer-experience.md) |

## Coordination

See [docs/agents/COORDINATION.md](./docs/agents/COORDINATION.md) for:
- Decision process and conflict resolution
- Sync points between agents
- Definition of done

## Conventions

These apply to **all** work in this repository:

1. **Result-based APIs** - Session/manager methods return `Result<T>`, UI uses `Result.fold(...)`
2. **Flow-first observation** - ViewModels expose `StateFlow`, UI collects with `collectAsState()`
3. **State persistence** - Lifecycle transitions must call `SessionRepository.updateSessionState()`
4. **Logging** - Use `Timber` consistently
5. **No invented architecture** - Only document what exists in-tree; mark planned work explicitly
