#!/bin/sh
# Devbox setup script for Alpine Linux
# This script sets up the user environment and installs base packages

set -e

echo "[devbox] Starting devbox setup..."

# Create udroid user if not exists
if ! id -u udroid > /dev/null 2>&1; then
    echo "[devbox] Creating udroid user..."
    adduser -D -s /bin/sh udroid
    echo "udroid:udroid" | chpasswd
    echo "[devbox] User udroid created"
else
    echo "[devbox] User udroid already exists"
fi

# Update package index
echo "[devbox] Updating package index..."
apk update

# Install base packages
echo "[devbox] Installing base packages..."
apk add --no-cache \
    dropbear \
    python3 \
    py3-pip \
    openssh-client \
    curl \
    wget \
    git \
    busybox-extras

# Setup dropbear (SSH server)
echo "[devbox] Setting up SSH server..."
mkdir -p /etc/dropbear
if [ ! -f /etc/dropbear/dropbear_rsa_host_key ]; then
    dropbearkey -t rsa -f /etc/dropbear/dropbear_rsa_host_key
    echo "[devbox] Generated SSH host key"
fi

# Setup user SSH directory
echo "[devbox] Setting up user SSH directory..."
mkdir -p /home/udroid/.ssh
chown udroid:udroid /home/udroid/.ssh
chmod 700 /home/udroid/.ssh

# Generate user SSH key if not exists
if [ ! -f /home/udroid/.ssh/id_rsa ]; then
    ssh-keygen -t rsa -b 2048 -f /home/udroid/.ssh/id_rsa -N "" -C "udroid@devbox"
    chown udroid:udroid /home/udroid/.ssh/id_rsa /home/udroid/.ssh/id_rsa.pub
    chmod 600 /home/udroid/.ssh/id_rsa
    chmod 644 /home/udroid/.ssh/id_rsa.pub
    echo "[devbox] Generated user SSH key"
fi

# Setup home directory
echo "[devbox] Setting up home directory..."
mkdir -p /home/udroid/workspace
chown -R udroid:udroid /home/udroid

# Install Tailscale utility
SCRIPT_DIR=$(dirname "$0")
if [ -f "$SCRIPT_DIR/setup-tailscale.sh" ]; then
    echo "[devbox] Installing Tailscale setup utility..."
    cp "$SCRIPT_DIR/setup-tailscale.sh" /usr/local/bin/setup-tailscale
    chmod +x /usr/local/bin/setup-tailscale
fi

# Create marker file
echo "[devbox] Setup complete!"
touch /home/udroid/.devbox-setup-complete

echo "[devbox] ============================================"
echo "[devbox] Devbox setup complete!"
echo "[devbox] User: udroid"
echo "[devbox] Password: udroid"
echo "[devbox] Workspace: /home/udroid/workspace"
echo "[devbox] Remote Access: run 'sudo setup-tailscale' to configure"
echo "[devbox] ============================================"
