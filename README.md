# Ubuntu on Android

Run Ubuntu on Android without root using PRoot.

## Status

🚧 **Under Active Development**

This project is transforming the Termux-based udroid into a native Android application.

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for complete system architecture.

## Development

### Prerequisites

- Android Studio Hedgehog | 2023.1.1 or later
- JDK 17
- Android NDK 25.1.8937393 or later
- Gradle 8.2

### Build

```bash
# Clone the repository
git clone https://github.com/yourusername/ubuntu-on-android.git
cd ubuntu-on-android

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

### Project Structure

```
app/                           # Android application module
  src/main/
    java/com/udroid/app/
      model/                   # Data models
      session/                 # Session management
      ui/                      # Compose UI
      service/                 # Android services
      native/                  # JNI bridges
native/                        # Native libraries
  proot/                       # PRoot for Android
  vnc-bridge/                  # VNC client bridge
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and [AGENTS.md](AGENTS.md) for development guidelines.

## License

MIT License - see [LICENSE](LICENSE) for details.

## Acknowledgments

Based on [udroid](https://github.com/RandomCoderOrg/ubuntu-on-android) by RandomCoderOrg.

Uses PRoot for rootless chroot environments.

---

**Note:** This project is not affiliated with Canonical or Ubuntu.
