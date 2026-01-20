#!/bin/sh
# Agent environment setup script
# Run this to activate the agent environment in a session
# Includes: pk-puzldai, factory.ai droid CLI

set -e

VENV_DIR="/opt/agent-tools/venv"
CONFIG_DIR="/home/udroid/.config/pk-puzldai"

# Check if agent tools are installed
if [ ! -d "$VENV_DIR" ]; then
    echo "[agent-env] Agent tools are not installed"
    echo "[agent-env] Run install-pk-puzldai.sh first"
    exit 1
fi

# Activate virtual environment
echo "[agent-env] Activating agent environment..."
. "$VENV_DIR/bin/activate"

# Add local bin to PATH
export PATH="/usr/local/bin:$PATH"

# Set agent tools home
export AGENT_TOOLS_HOME="/opt/agent-tools"
export PK_PUZLDAI_HOME="/opt/agent-tools"

# Load user config if exists
if [ -f "$CONFIG_DIR/env" ]; then
    echo "[agent-env] Loading user environment..."
    . "$CONFIG_DIR/env"
fi

# Check for API key
if [ -z "$ANTHROPIC_API_KEY" ]; then
    echo "[agent-env] WARNING: ANTHROPIC_API_KEY is not set"
    echo "[agent-env] Set it with: export ANTHROPIC_API_KEY='your-key'"
fi

echo "[agent-env] Agent environment activated"
echo "[agent-env] Available commands:"
echo "  pk-puzldai    - pk-puzldai agent CLI"
echo "  droid         - factory.ai droid CLI"
echo "  gemini        - Google Gemini CLI"
echo "  agent-run     - Run agent tasks"
echo "  python3       - Python with agent packages"

# If arguments provided, run them
if [ $# -gt 0 ]; then
    exec "$@"
fi
