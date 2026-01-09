# Android Project Scaffold - Ubuntu on Android

This document provides the initial Android project structure and configuration files for the Ubuntu-on-Android standalone app.

---

## Project Structure

```
ubuntu-on-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/udroid/app/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── UdroidApplication.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── setup/
│   │   │   │   │   │   ├── SetupWizardActivity.kt
│   │   │   │   │   │   ├── DistroSelectionFragment.kt
│   │   │   │   │   │   └── SetupViewModel.kt
│   │   │   │   │   ├── session/
│   │   │   │   │   │   ├── SessionListActivity.kt
│   │   │   │   │   │   ├── SessionDetailsActivity.kt
│   │   │   │   │   │   ├── SessionListAdapter.kt
│   │   │   │   │   │   └── SessionListViewModel.kt
│   │   │   │   │   ├── desktop/
│   │   │   │   │   │   ├── DesktopActivity.kt
│   │   │   │   │   │   ├── DesktopSurfaceView.kt
│   │   │   │   │   │   └── DesktopViewModel.kt
│   │   │   │   │   └── settings/
│   │   │   │   │       ├── SettingsActivity.kt
│   │   │   │   │       └── SettingsFragment.kt
│   │   │   │   ├── service/
│   │   │   │   │   ├── UbuntuSessionService.kt
│   │   │   │   │   ├── RootfsDownloadService.kt
│   │   │   │   │   └── VncBridgeService.kt
│   │   │   │   ├── session/
│   │   │   │   │   ├── UbuntuSessionManager.kt
│   │   │   │   │   ├── UbuntuSession.kt
│   │   │   │   │   └── SessionConfig.kt
│   │   │   │   ├── rootfs/
│   │   │   │   │   ├── RootfsManager.kt
│   │   │   │   │   ├── RootfsDownloader.kt
│   │   │   │   │   └── RootfsExtractor.kt
│   │   │   │   ├── storage/
│   │   │   │   │   ├── StorageManager.kt
│   │   │   │   │   └── SessionRepository.kt
│   │   │   │   ├── native/
│   │   │   │   │   ├── NativeBridge.kt
│   │   │   │   │   ├── ProotLauncher.kt
│   │   │   │   │   └── JniLibraryLoader.kt
│   │   │   │   └── model/
│   │   │   │       ├── DistroVariant.kt
│   │   │   │       ├── SessionState.kt
│   │   │   │       └── SessionInfo.kt
│   │   │   ├── res/
│   │   │   │   ├── drawable/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_main.xml
│   │   │   │   │   ├── activity_setup_wizard.xml
│   │   │   │   │   ├── activity_session_list.xml
│   │   │   │   │   ├── activity_desktop.xml
│   │   │   │   │   ├── item_session.xml
│   │   │   │   │   └── fragment_settings.xml
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   ├── themes.xml
│   │   │   │   │   └── dimens.xml
│   │   │   │   └── xml/
│   │   │   │       └── network_security_config.xml
│   │   │   ├── jni/
│   │   │   │   ├── proot/
│   │   │   │   ├── vnc-bridge/
│   │   │   │   └── lib/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/
│   │   │   └── java/com/udroid/app/
│   │   │       ├── session/
│   │   │       │   └── UbuntuSessionManagerTest.kt
│   │   │       └── rootfs/
│   │   │           └── RootfsManagerTest.kt
│   │   └── androidTest/
│   │       └── java/com/udroid/app/
│   │           └── ui/
│   │               └── SessionListActivityTest.kt
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   ├── wrapper/
│   │   └── gradle-wrapper.properties
│   └── libs.versions.toml
├── native/
│   ├── proot/
│   │   ├── CMakeLists.txt
│   │   └── src/
│   ├── vnc-bridge/
│   │   ├── CMakeLists.txt
│   │   └── src/
│   └── build-native.sh
├── scripts/
│   ├── install-device.sh
│   ├── mock-rootfs.sh
│   └── run-emulator.sh
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## Core Configuration Files

### 1. Root `build.gradle.kts`

```kotlin
// Top-level build file
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
}
```

### 2. Root `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Ubuntu-on-Android"
include(":app")
include(":native:proot")
include(":native:vnc-bridge")
```

### 3. Root `gradle.properties`

```properties
# Project-wide Gradle settings.
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true

# Android
android.useAndroidX=true
android.enableJetifier=true

# Kotlin
kotlin.code.style=official
kotlin.mpp.enableCInteropCommonization=true

# Signing (set in local.properties or env)
RELEASE_STORE_FILE=
RELEASE_STORE_PASSWORD=
RELEASE_KEY_ALIAS=
RELEASE_KEY_PASSWORD=

# Versioning
VERSION_MAJOR=0
VERSION_MINOR=1
VERSION_PATCH=0
VERSION_BUILD=0
```

### 4. App `build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.udroid.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.udroid.app"
        minSdk = 26
        targetSdk = 34
        versionCode = VERSION_MAJOR * 10000 + VERSION_MINOR * 100 + VERSION_PATCH * 10 + VERSION_BUILD
        versionName = "${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86_64"))
        }

        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_TOOLCHAIN=clang"
                )
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            isMinifyEnabled = false
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        create("staging") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            isMinifyEnabled = true
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("full") {
            dimension = "distribution"
            isDefault = true
        }

        create("minimal") {
            dimension = "distribution"
            applicationIdSuffix = ".minimal"
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")

    // Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.3")

    // Work Manager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // Preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // JSON
    implementation("com.google.gson:gson:2.10.1")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("app.cash.turbine:turbine:1.0.0")

    // Instrumentation Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

### 5. App `AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- For Android 12 and below -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />

    <!-- For Android 13+ -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES"
        android:minSdkVersion="33" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO"
        android:minSdkVersion="33" />

    <application
        android:name=".UdroidApplication"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Udroid"
        android:usesCleartextTraffic="false"
        tools:targetApi="34">

        <!-- Main Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Udroid">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Setup Wizard Activity -->
        <activity
            android:name=".ui.setup.SetupWizardActivity"
            android:exported="false"
            android:theme="@style/Theme.Udroid" />

        <!-- Session List Activity -->
        <activity
            android:name=".ui.session.SessionListActivity"
            android:exported="false"
            android:theme="@style/Theme.Udroid" />

        <!-- Desktop Activity -->
        <activity
            android:name=".ui.desktop.DesktopActivity"
            android:exported="false"
            android:theme="@style/Theme.Udroid.FullScreen"
            android:configChanges="orientation|keyboardHidden|screenSize"
            android:screenOrientation="landscape"
            android:hardwareAccelerated="true" />

        <!-- Settings Activity -->
        <activity
            android:name=".ui.settings.SettingsActivity"
            android:exported="false"
            android:theme="@style/Theme.Udroid" />

        <!-- Ubuntu Session Service -->
        <service
            android:name=".service.UbuntuSessionService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="ubuntu_session" />
        </service>

        <!-- Rootfs Download Service -->
        <service
            android:name=".service.RootfsDownloadService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="dataProcessing" />

        <!-- VNC Bridge Service -->
        <service
            android:name=".service.VncBridgeService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="vnc_bridge" />
        </service>

        <!-- File provider for sharing -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>

</manifest>
```

---

## Key Interface Definitions

### UbuntuSessionManager Interface

```kotlin
package com.udroid.app.session

import kotlinx.coroutines.flow.Flow

/**
 * Manages Ubuntu sessions lifecycle.
 */
interface UbuntuSessionManager {
    /**
     * Creates a new Ubuntu session with the given configuration.
     */
    suspend fun createSession(config: SessionConfig): Result<UbuntuSession>

    /**
     * Lists all existing sessions.
     */
    fun listSessions(): Flow<List<UbuntuSession>>

    /**
     * Gets a session by ID.
     */
    fun getSession(sessionId: String): UbuntuSession?

    /**
     * Deletes a session.
     */
    suspend fun deleteSession(sessionId: String): Result<Unit>

    /**
     * Starts a session.
     */
    suspend fun startSession(sessionId: String): Result<Unit>

    /**
     * Stops a running session.
     */
    suspend fun stopSession(sessionId: String): Result<Unit>
}

/**
 * Represents a Ubuntu session.
 */
interface UbuntuSession {
    val id: String
    val config: SessionConfig
    val state: SessionState
    val stateFlow: Flow<SessionState>

    suspend fun start(): Result<Unit>
    suspend fun stop(): Result<Unit>
    suspend fun exec(command: String): Result<ProcessResult>
}

/**
 * Session configuration.
 */
data class SessionConfig(
    val name: String,
    val distro: DistroVariant,
    val desktopEnvironment: DesktopEnvironment,
    val resolution: Resolution,
    val enableSound: Boolean = true,
    val enableNetwork: Boolean = true
)

/**
 * Session state.
 */
sealed class SessionState {
    data object Created : SessionState()
    data object Starting : SessionState()
    data class Running(val vncPort: Int) : SessionState()
    data object Stopping : SessionState()
    data object Stopped : SessionState()
    data class Error(val message: String) : SessionState()
}

/**
 * Process result.
 */
data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
)
```

### DistroVariant Enum

```kotlin
package com.udroid.app.model

/**
 * Ubuntu distribution variants.
 */
enum class DistroVariant(
    val id: String,
    val displayName: String,
    val version: UbuntuVersion,
    val downloadUrl: String,
    val sha256: String,
    val sizeBytes: Long
) {
    JAMMY_XFCE4(
        id = "jammy:xfce4",
        displayName = "Ubuntu 22.04 LTS with XFCE4",
        version = UbuntuVersion.JAMMY,
        downloadUrl = "https://github.com/RandomCoderOrg/udroid-download/releases/download/V3R3/jammy-xfce4-arm64.tar.gz",
        sha256 = "...", // Replace with actual checksum
        sizeBytes = 2_500_000_000L // ~2.5GB
    ),
    JAMMY_MATE(
        id = "jammy:mate",
        displayName = "Ubuntu 22.04 LTS with MATE",
        version = UbuntuVersion.JAMMY,
        downloadUrl = "https://github.com/RandomCoderOrg/udroid-download/releases/download/V3R3/jammy-mate-arm64.tar.gz",
        sha256 = "...",
        sizeBytes = 2_400_000_000L
    ),
    JAMMY_GNOME(
        id = "jammy:gnome",
        displayName = "Ubuntu 22.04 LTS with GNOME",
        version = UbuntuVersion.JAMMY,
        downloadUrl = "https://github.com/RandomCoderOrg/udroid-download/releases/download/V3R3/jammy-gnome-arm64.tar.gz",
        sha256 = "...",
        sizeBytes = 3_000_000_000L
    ),
    NOBLE_RAW(
        id = "noble:raw",
        displayName = "Ubuntu 24.04 LTS (CLI only)",
        version = UbuntuVersion.NOBLE,
        downloadUrl = "https://github.com/RandomCoderOrg/udroid-download/releases/download/V4R0/noble-raw-arm64.tar.gz",
        sha256 = "...",
        sizeBytes = 500_000_000L
    );

    companion object {
        fun fromId(id: String): DistroVariant? =
            values().find { it.id == id }
    }
}

enum class UbuntuVersion(val versionNumber: String) {
    FOCAL("20.04"),
    JAMMY("22.04"),
    NOBLE("24.04")
}

enum class DesktopEnvironment(val displayName: String) {
    NONE("CLI Only"),
    XFCE4("XFCE4"),
    MATE("MATE"),
    GNOME("GNOME")
}
```

---

## JNI Bridge Signatures

```kotlin
package com.udroid.app.native

/**
 * Native bridge for launching and managing PRoot processes.
 */
object NativeBridge {
    init {
        System.loadLibrary("udroid-native")
    }

    /**
     * Launches a PRoot session.
     *
     * @param rootfsPath Absolute path to rootfs directory
     * @param sessionDir Absolute path to session directory
     * @param bindMounts Array of bind mount specifications ("src:dest")
     * @param envVars Environment variables as "KEY=VALUE" strings
     * @param command Command to execute inside PRoot
     * @return Process ID of launched PRoot process
     */
    external fun launchProot(
        rootfsPath: String,
        sessionDir: String,
        bindMounts: Array<String>,
        envVars: Array<String>,
        command: String
    ): Long

    /**
     * Waits for a PRoot process to exit.
     *
     * @param pid Process ID from launchProot
     * @param timeoutMs Timeout in milliseconds (0 = infinite)
     * @return Exit code
     */
    external fun waitForProot(pid: Long, timeoutMs: Long): Int

    /**
     * Sends a signal to a PRoot process.
     *
     * @param pid Process ID
     * @param signal Signal number (e.g., 15 for SIGTERM)
     */
    external fun killProot(pid: Long, signal: Int): Boolean

    /**
     * Gets the stdout from a PRoot process.
     *
     * @param pid Process ID
     * @return Stdout content
     */
    external fun getProotStdout(pid: Long): String

    /**
     * Gets the stderr from a PRoot process.
     *
     * @param pid Process ID
     * @return Stderr content
     */
    external fun getProotStderr(pid: Long): String

    /**
     * Checks if a PRoot process is running.
     *
     * @param pid Process ID
     * @return true if running, false otherwise
     */
    external fun isProotRunning(pid: Long): Boolean
}
```

---

## Next Steps for Implementation

### Week 1-2: Foundation Setup
1. Create project structure using `gradle init`
2. Copy configuration files
3. Setup native build infrastructure
4. Create stub implementations for all interfaces
5. Setup basic UI navigation

### Week 3-4: Core Services
1. Implement UbuntuSessionService
2. Implement RootfsDownloadService
3. Implement basic session management
4. Setup dependency injection

### Week 5-8: Native Integration
1. Port PRoot to Android
2. Implement JNI bridge
3. Test PRoot functionality
4. Implement session launcher

### Week 9-12: GUI Implementation
1. Implement VNC client
2. Create DesktopSurfaceView
3. Implement input translation
4. Connect VNC bridge

### Week 13-16: Polish & Testing
1. End-to-end integration
2. Performance optimization
3. Testing and bug fixes
4. Documentation

---

## Build Commands

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest

# Build native libraries
./native/build-native.sh

# Generate APK for all architectures
./gradlew assembleRelease --abi-split=true
```

---

## Notes

- This scaffold uses Jetpack Compose for modern Android UI
- Hilt for dependency injection
- Kotlin Coroutines + Flow for async operations
- Material 3 for theming
- Native libraries will be built separately using CMake
- App targets Android 8.0+ (API 26+) for broad compatibility
