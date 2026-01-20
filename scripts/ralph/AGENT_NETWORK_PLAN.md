# Ubuntu on Android - Agent Network & Usability Plan

## Current Status

### Working ✅
- Multi-process support implemented (6 stories complete)
- Ubuntu 24.04 LTS rootfs extracted and functional
- PRoot executes commands successfully
- Python 3.12, curl, apt available in rootfs
- Session lifecycle management working

### Blocked 🔴
- **Network unreachable from proot** - "Network is unreachable" error
  - Not DNS issue (resolv.conf configured correctly)
  - Fundamental proot syscall interception limitation on this Android device
  - IP connectivity fails: `curl http://142.250.80.46` → "Network is unreachable"

### Missing for Agent Workflows
1. Network access for API calls (Claude API, package managers, etc.)
2. Terminal UI in app for direct interaction
3. Node.js for Claude Code CLI
4. Agent wrapper UI (hide Linux implementation)

---

## Problem Analysis: Network in PRoot

PRoot intercepts syscalls but doesn't have true network namespace isolation. On some Android devices/versions, the network stack isn't accessible through proot's emulated syscalls.

### Potential Solutions

1. **Native Network Proxy** (Recommended)
   - Android app acts as HTTP/SOCKS proxy
   - PRoot environment configured to use `localhost:PORT` proxy
   - All network traffic routed through Android's network stack
   - Implementation: OkHttp server in Android, `http_proxy` env var in proot

2. **VirtIO/TAP Networking**
   - Requires root access (available on this device)
   - Create TAP interface for proot
   - More complex but provides full network stack

3. **Termux-style Helper**
   - Separate process handles network requests
   - IPC between proot and helper
   - How Termux solves this problem

4. **Use Alpine Instead**
   - Alpine has proven proot compatibility
   - Already bundled in app
   - Test if network works there first

---

## Implementation Plan

### Phase 1: Diagnose & Quick Win (1-2 hours)
1. Test network in bundled Alpine rootfs
2. If Alpine works, use Alpine as base for agent environment
3. If Alpine fails too, proceed to proxy solution

### Phase 2: Native Network Proxy (if needed) (4-6 hours)
1. Add OkHttp proxy server to Android app
2. Start proxy when session starts
3. Configure proot environment to use proxy
4. Test API calls to Anthropic

### Phase 3: Agent Environment Setup (2-3 hours)
1. Install Node.js in working rootfs (via apt or nvm)
2. Install Claude Code CLI
3. Configure API key storage (secure Android keystore)
4. Test basic Claude Code operation

### Phase 4: Terminal UI (4-6 hours)
1. Add Terminal composable to app
2. Connect to session exec() method
3. Handle stdin/stdout streaming
4. Basic ANSI rendering

### Phase 5: Agent Wrapper UI (8+ hours)
1. Design agent interaction UI
2. Chat-style interface for Claude Code
3. Task queue visualization
4. Hide proot/Linux complexity from user

---

## Immediate Next Steps

1. **Test Alpine networking** - Quick validation
2. **If Alpine works**: Use Alpine + apk to install Node.js
3. **If Alpine fails**: Implement native proxy solution
4. **Parallel**: Add basic terminal UI for debugging

---

## Technical Notes

### PRoot Networking Limitation
```
curl -v http://142.250.80.46
* Trying 142.250.80.46:80...
* Immediate connect fail for 142.250.80.46: Network is unreachable
```

This is NOT:
- DNS issue (resolv.conf is correct)
- Firewall issue (Android can reach network)
- Permission issue (app has INTERNET permission)

This IS:
- PRoot syscall interception not forwarding network calls correctly
- Android's network stack not accessible via emulated syscalls

### Native Proxy Architecture
```
[PRoot Ubuntu]
    |
    | HTTP request via env HTTP_PROXY=localhost:8118
    v
[Android App - OkHttp Proxy Server on :8118]
    |
    | Native Android network call
    v
[Internet]
```

This bypasses proot's syscall interception for network calls entirely.
