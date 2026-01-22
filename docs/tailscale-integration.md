# Tailscale Integration

This project includes support for connecting your Ubuntu session to a Tailscale mesh network, turning your Android device into a remotely accessible dev box.

## Overview

The integration uses `tailscaled` in userspace networking mode, allowing it to run inside the PROOT environment without requiring root access on the Android host (though the session itself runs as an emulated root).

## Prerequisites

- An active Ubuntu session.
- `setup-devbox.sh` has been run (usually part of the session initialization).

## Setup Instructions

1.  **Start your session.**
2.  **Open the terminal** inside the session.
3.  **Run the setup script:**
    ```bash
    sudo setup-tailscale
    ```
    *(Note: `sudo` is implied if you are already root in the session)*

4.  **Authenticate:**
    The script will install Tailscale (if missing) and start the daemon.
    It will print a login URL.
    - Copy the URL.
    - Open it in a browser on your Android device or another computer.
    - Authorize the device.

5.  **Access:**
    Once authenticated, the script will show your Tailscale IP address.
    You can now SSH into your session from other devices on your Tailnet:
    ```bash
    ssh udroid@<tailscale-ip>
    ```

## Implementation Details

- **Script Location:** `/usr/local/bin/setup-tailscale` (installed by `setup-devbox.sh`).
- **Daemon:** `tailscaled` runs in the background using a custom socket at `/var/run/tailscale/tailscaled.sock`.
- **Mode:** Userspace networking (`--tun=userspace-networking`) is used to bypass lack of `/dev/net/tun`.

## Troubleshooting

- **Daemon not starting:** Check `/var/run/tailscale` permissions.
- **Connectivity issues:** Ensure your Android device has active internet. PROOT networking relies on the host.
