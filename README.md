# Ubuntu on Android (Standalone App)

A work-in-progress **native Android app** that aims to run Ubuntu **inside the app sandbox without root**.

This repository is a rewrite/transition away from the original Termux-driven workflow toward an APK-managed experience (session management, persistence, services, and eventually native PRoot + desktop rendering).

## Status

**Under active development.**

What is implemented today is primarily the **Android app shell** (UI + persistence + service wiring). The “Ubuntu runtime” is still stubbed.

## What works today (implemented)

- **Session CRUD + persistence** (sessions are stored via DataStore)
- **Compose UI screens**
  - Setup wizard (select distro + name)
  - Session list (start/stop/delete)
  - Desktop screen placeholder
- **Foreground service plumbing** (`UbuntuSessionService`) with start/stop actions

## What is stubbed / not implemented yet

- **Actual Ubuntu execution** (no real PRoot process launch yet)
- **Rootfs download / verification / extraction**
- **Desktop rendering pipeline** (current `DesktopScreen` shows a placeholder)

If you are looking for a working Termux-based Ubuntu-on-Android experience today, see the upstream project referenced in the citations below.

## Architecture

- High-level design: [ARCHITECTURE.md](ARCHITECTURE.md)
- Agent/ownership boundaries + authoritative code snapshot: [AGENTS.md](AGENTS.md)

## Developer Quick Start

### Prerequisites

- Android Studio (JDK **17**)
- Android SDK installed via Android Studio
- Android NDK (the repo historically references **25.1.8937393**)

### Open & run

1. Open this repository in Android Studio.
2. Let Gradle sync.
3. Select the `app` run configuration.
4. Run on an emulator or device.

### Build from command line

On Windows:

```powershell
./gradlew.bat :app:assembleDebug
./gradlew.bat :app:installDebug
```

On macOS/Linux:

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

## Codebase map (where to start reading)

- **Application + logging**: `app/src/main/java/com/udroid/app/UdroidApplication.kt`
- **Navigation + permissions gate**: `app/src/main/java/com/udroid/app/MainActivity.kt`
- **Session API contract**: `app/src/main/java/com/udroid/app/session/UbuntuSession.kt`
- **Session implementation**: `app/src/main/java/com/udroid/app/session/UbuntuSessionManagerImpl.kt`
- **Persistence**: `app/src/main/java/com/udroid/app/storage/SessionRepository.kt`
- **Foreground service**: `app/src/main/java/com/udroid/app/service/UbuntuSessionService.kt`
- **Native bridge stub**: `app/src/main/java/com/udroid/app/nativebridge/NativeBridge.kt`

## Contributing

- See [CONTRIBUTING.md](CONTRIBUTING.md)
- For repo-specific assistant behavior and “keep context pristine” rules, see:
  - [CLAUDE.md](CLAUDE.md)
  - [GEMINI.md](GEMINI.md)

## Attribution / citations

- Original project (Termux-based):
  - RandomCoderOrg, **ubuntu-on-android**: https://github.com/RandomCoderOrg/ubuntu-on-android

- Upstream udroid ecosystem repositories referenced by this repo (see `.gitmodules`):
  - RandomCoderOrg, **udroid-extra-tool-proot**: https://github.com/RandomCoderOrg/udroid-extra-tool-proot
  - RandomCoderOrg, **fs-manager-udroid**: https://github.com/RandomCoderOrg/fs-manager-udroid
  - RandomCoderOrg, **fs-cook**: https://github.com/RandomCoderOrg/fs-cook
  - RandomCoderOrg, **udroid-download**: https://github.com/RandomCoderOrg/udroid-download

- Related upstream projects used by the Termux-based workflow and/or referenced in docs:
  - **PRoot**: https://github.com/proot-me/proot
  - **proot-distro** (Termux): https://github.com/termux/proot-distro
  - **Termux**: https://github.com/termux/termux-app

This repository is a standalone-app transformation effort that is **inspired by and derived from the concepts/workflow** of the upstream Termux-based project(s) above (often referred to as “udroid” in docs).

## License

MIT License — see [LICENSE](LICENSE).

---

**Note:** This project is not affiliated with Canonical or Ubuntu.
