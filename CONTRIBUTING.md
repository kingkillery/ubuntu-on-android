# Contributing

This repository is the **standalone Android app transformation** of the Termux-based udroid workflow.

## Code of Conduct

Participation is governed by [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## Where to start

- Project overview + build/run instructions: [README.md](README.md)
- Architecture: [ARCHITECTURE.md](ARCHITECTURE.md)
- Ownership boundaries + authoritative code snapshot: [AGENTS.md](AGENTS.md)
- Repo-specific assistant operation rules:
  - [CLAUDE.md](CLAUDE.md)
  - [GEMINI.md](GEMINI.md)

## How to contribute

- Follow the basic GitHub PR workflow guide: https://docs.github.com/en/get-started/quickstart/hello-world
- Keep PRs focused.
- Include reproduction steps and screenshots for UI changes when possible.

## Repo conventions

- **Kotlin + Coroutines + Flow** for async/state.
- **Compose** UI; ViewModels own state (`StateFlow`), composables collect via `collectAsState()`.
- **Hilt** dependency injection.
- **Result-based APIs** for session operations; avoid throwing across UI/service boundaries.
- **Persistence** uses DataStore Preferences (see `app/src/main/java/com/udroid/app/storage/SessionRepository.kt`).

## Commit messages

One line is fine for small changes. For larger changes, include:

```
<summary>

<what changed>
<why>
<impact>
```

## Upstream references

This repo cites and is influenced by upstream projects in the udroid ecosystem. For reference:

- RandomCoderOrg `ubuntu-on-android`: https://github.com/RandomCoderOrg/ubuntu-on-android
- RandomCoderOrg `fs-manager-udroid`: https://github.com/RandomCoderOrg/fs-manager-udroid
- RandomCoderOrg `udroid-wiki`: https://github.com/RandomCoderOrg/udroid-wiki
- RandomCoderOrg `fs-cook`: https://github.com/RandomCoderOrg/fs-cook
