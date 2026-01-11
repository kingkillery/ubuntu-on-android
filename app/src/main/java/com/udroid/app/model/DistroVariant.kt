package com.udroid.app.model

enum class UbuntuVersion(val versionNumber: String) {
    FOCAL("20.04"),
    JAMMY("22.04"),
    NOBLE("24.04"),
    ALPINE("3.21")
}

enum class DesktopEnvironment(val displayName: String) {
    NONE("CLI Only"),
    XFCE4("XFCE4"),
    MATE("MATE"),
    GNOME("GNOME")
}

enum class DistroVariant(
    val id: String,
    val displayName: String,
    val version: UbuntuVersion,
    val desktop: DesktopEnvironment,
    val sizeBytes: Long,
    val bundled: Boolean = false,
    val assetPath: String? = null,
    val downloadUrl: String? = null
) {
    JAMMY_XFCE4(
        id = "jammy:xfce4",
        displayName = "Ubuntu 22.04 LTS with XFCE4",
        version = UbuntuVersion.JAMMY,
        desktop = DesktopEnvironment.XFCE4,
        sizeBytes = 2_500_000_000L,
        downloadUrl = "https://cloud-images.ubuntu.com/jammy/current/jammy-server-cloudimg-arm64-root.tar.xz"
    ),
    JAMMY_MATE(
        id = "jammy:mate",
        displayName = "Ubuntu 22.04 LTS with MATE",
        version = UbuntuVersion.JAMMY,
        desktop = DesktopEnvironment.MATE,
        sizeBytes = 2_400_000_000L,
        downloadUrl = "https://cloud-images.ubuntu.com/jammy/current/jammy-server-cloudimg-arm64-root.tar.xz"
    ),
    JAMMY_GNOME(
        id = "jammy:gnome",
        displayName = "Ubuntu 22.04 LTS with GNOME",
        version = UbuntuVersion.JAMMY,
        desktop = DesktopEnvironment.GNOME,
        sizeBytes = 3_000_000_000L,
        downloadUrl = "https://cloud-images.ubuntu.com/jammy/current/jammy-server-cloudimg-arm64-root.tar.xz"
    ),
    NOBLE_RAW(
        id = "noble:raw",
        displayName = "Ubuntu 24.04 LTS (CLI only)",
        version = UbuntuVersion.NOBLE,
        desktop = DesktopEnvironment.NONE,
        sizeBytes = 210_000_000L,
        downloadUrl = "https://cloud-images.ubuntu.com/noble/current/noble-server-cloudimg-arm64-root.tar.xz"
    ),
    ALPINE_MINI(
        id = "alpine:mini",
        displayName = "Alpine Linux (Minimal, Bundled)",
        version = UbuntuVersion.ALPINE,
        desktop = DesktopEnvironment.NONE,
        sizeBytes = 3_000_000L,
        bundled = true,
        assetPath = "rootfs/alpine-arm64.tar.gz.bin"
    );

    companion object {
        fun fromId(id: String): DistroVariant? =
            entries.find { it.id == id }
    }
}

sealed class SessionState {
    data object Created : SessionState()
    data object Starting : SessionState()
    data class Running(val vncPort: Int) : SessionState()
    data object Stopping : SessionState()
    data object Stopped : SessionState()
    data class Error(val message: String) : SessionState()
}

data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
)
