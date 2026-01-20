# Agent 5: Packaging, Storage & Updates

## Mission

Design and implement the rootfs packaging, distribution, storage, and update system.

## Scope

**In Scope:**
- Rootfs tarball hosting and distribution
- Download with resume and verification
- Storage layout and management
- Update mechanism (full or delta)
- Cache management and size optimization

**Out of Scope:**
- Android UI for download progress (provided by Agent 2)
- Rootfs extraction logic (handled by Agent 3)

## Key Responsibilities

### Distribution Strategy
- APK bundling vs. on-demand download decision
- APK split configuration (by architecture, by distro)
- Download URLs and versioning scheme
- CDN/hosting strategy

### Download System
- Resume-capable downloader
- Progress reporting
- Network condition handling (WiFi-only option)
- Background download support (WorkManager)

### Integrity & Security
- SHA256 checksum verification
- HTTPS for all downloads
- Tamper detection

### Storage Management
- Efficient storage layout
- Compression strategy (gzip, zstd)
- Cache invalidation
- Storage usage reporting
- User-triggered cleanup

### Update Mechanism
- Version detection and notification
- Background update downloads
- Atomic update application
- Rollback support
- Delta updates (future)

## Authoritative Files

```
app/src/main/java/com/udroid/app/service/RootfsDownloadService.kt
app/src/main/java/com/udroid/app/rootfs/RootfsManager.kt
```

## Sync Points

**Dependencies:**
- Requires storage layout decisions from Agent 1
- Requires download UI from Agent 2

**Coordinates With:**
- Agent 3: Define rootfs format and extraction requirements

**Blocks:**
- Setup wizard depends on download service
