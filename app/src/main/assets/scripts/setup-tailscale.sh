#!/bin/sh
# Setup Tailscale for remote access
# This script installs and configures Tailscale in the PROOT environment

set -e

echo "[tailscale] Starting Tailscale setup..."

# Check if running as root
if [ "$(id -u)" -ne 0 ]; then
    echo "[tailscale] Error: This script must be run as root"
    exit 1
fi

# Enable community repo if needed (Tailscale is usually in community)
if ! grep -q "community" /etc/apk/repositories; then
    echo "http://dl-cdn.alpinelinux.org/alpine/edge/community" >> /etc/apk/repositories
    apk update
fi

# Install Tailscale
if ! command -v tailscale > /dev/null; then
    echo "[tailscale] Installing Tailscale package..."
    apk update
    apk add tailscale
else
    echo "[tailscale] Tailscale is already installed"
fi

# Create directory for tailscaled socket and state
mkdir -p /var/run/tailscale
mkdir -p /var/lib/tailscale

# Check if tailscaled is running
if ! pgrep tailscaled > /dev/null; then
    echo "[tailscale] Starting tailscaled..."
    # Start tailscaled with userspace networking (required for PROOT)
    tailscaled --tun=userspace-networking --socket=/var/run/tailscale/tailscaled.sock --state=/var/lib/tailscale/tailscaled.state &
    
    # Wait for daemon to start
    echo "[tailscale] Waiting for daemon..."
    sleep 5
else
    echo "[tailscale] tailscaled is already running"
fi

echo "[tailscale] Bringing up Tailscale..."
echo "[tailscale] You will be asked to authenticate. Copy the link below if prompted."

# Run tailscale up
# We use --socket because we started it with a specific socket path
tailscale --socket=/var/run/tailscale/tailscaled.sock up

echo "[tailscale] Setup complete!"
echo "[tailscale] IP Address:"
tailscale --socket=/var/run/tailscale/tailscaled.sock ip -4
