# Agent 4: GUI & Interaction

## Mission

Implement the desktop display pipeline and input handling, providing a native Android surface rendering of the Ubuntu desktop environment with touch, keyboard, and gesture support.

## Scope

**In Scope:**
- Display strategy implementation (VNC bridge or alternative)
- SurfaceView rendering implementation
- Input event translation (touch, keyboard, mouse, gestures)
- IME (Input Method Editor) handling
- Performance optimization (frame rate, latency)

**Out of Scope:**
- VNC server setup (runs inside Ubuntu rootfs)
- Desktop environment configuration (part of rootfs)
- Android UI shell (handled by Agent 2)

## Key Responsibilities

### Display Pipeline
- Implement VNC client library (native or Java)
- Connect VNC client to Android SurfaceView
- Handle framebuffer updates and rendering
- Target 30+ FPS on mid-range devices

### Input Translation
- Touch events → Mouse pointer events
- Multi-touch gestures → Mouse wheel, right-click, drag
- Physical keyboard → X11 keysyms
- Soft keyboard (IME) → X11 input
- Special keys (Ctrl, Alt, Super, function keys)

### DesktopSurfaceView
- Custom SurfaceView for framebuffer rendering
- Touch event interception and translation
- Keyboard event handling
- Zoom and pan support (optional)

### Performance Optimization
- Frame skipping under load
- Differential framebuffer updates
- Hardware-accelerated rendering (OpenGL/Vulkan)

### Input Method Handling
- Show/hide soft keyboard on demand
- Handle IME composition
- Clipboard integration (Android ↔ Ubuntu)

## Authoritative Files

```
app/src/main/java/com/udroid/app/ui/desktop/DesktopScreen.kt (placeholder)
```

## Deliverables

- **VNC Client** - RFB protocol, connection management, framebuffer decoding
- **DesktopSurfaceView** - Rendering, touch/keyboard handling
- **InputHandler** - Touch→pointer, gesture recognition, keysym translation
- **VncBridgeService** - VNC lifecycle, connection, input forwarding

## Sync Points

**Dependencies:**
- Requires display strategy decision from Agent 1
- Requires DesktopScreen structure from Agent 2
- Requires session startup coordination from Agent 3

**Blocks:**
- End-to-end user flows depend on GUI completion
