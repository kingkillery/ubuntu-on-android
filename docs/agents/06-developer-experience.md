# Agent 6: Developer Experience & Automation

## Mission

Create tooling, scripts, and documentation for efficient development, testing, building, and releasing.

## Scope

**In Scope:**
- Build system configuration (Gradle, CMake, NDK)
- CI/CD pipeline setup
- Local development tooling
- Testing infrastructure
- Contributor documentation
- Release automation

**Out of Scope:**
- Application feature implementation
- Native library implementation (build setup only)

## Key Responsibilities

### Build System
- Gradle configuration for app and native libraries
- CMake setup for native components
- APK split configuration
- Signing configuration
- Build variants (debug, release, staging)

### CI/CD Pipeline
- GitHub Actions configuration
- Automated builds on commit
- Automated testing (unit, integration)
- APK artifact generation
- Release automation (tag → APK → GitHub release)

### Local Development Tools
- Quick-run scripts
- Mock rootfs generator for UI testing
- Debug logging setup
- ADB helper scripts

### Testing Infrastructure
- Unit test setup (JUnit, MockK)
- Instrumentation test setup (AndroidX Test)
- Mock native library for testing
- Test data fixtures
- Coverage reporting (JaCoCo)

### Documentation
- Contributor guide
- Build instructions
- Architecture overview
- API documentation

## Authoritative Files

```
app/build.gradle.kts
native/CMakeLists.txt
scripts/build-native.sh
.github/workflows/ (if exists)
```

## Deliverables

- **Build Configuration** - Gradle, CMake, ProGuard rules
- **CI/CD Pipeline** - GitHub Actions, Docker build environment
- **Development Scripts** - install-device.sh, mock-rootfs.sh
- **Testing Setup** - JUnit, MockK, Espresso, JaCoCo
- **Documentation** - README, CONTRIBUTING, BUILD, TESTING guides
