#!/bin/sh
# Agent tools installation script for Alpine Linux (proot environment)
# This script installs pk-puzldai (Node.js CLI), and dependencies for agent orchestration
# Updated for Node.js-based pk-puzldai TUI
#
# Priority: Bundled local build > npm registry > GitHub

set -e

INSTALL_DIR="/opt/pk-puzldai"
NODE_VERSION="20"
MARKER_FILE="/home/udroid/.pk-puzldai-installed"
LOG_FILE="/tmp/pk-puzldai-install.log"
BUNDLED_DIR="/tmp/pk-puzldai-bundle"  # Where Android app copies bundled files

log() {
    echo "[pk-puzldai] $1" | tee -a "$LOG_FILE"
}

error() {
    echo "[pk-puzldai] ERROR: $1" | tee -a "$LOG_FILE"
    exit 1
}

# Check if already installed
if [ -f "$MARKER_FILE" ]; then
    log "pk-puzldai is already installed"
    log "To reinstall, remove $MARKER_FILE and run again"
    exit 0
fi

log "Starting pk-puzldai installation..."
log "Install directory: $INSTALL_DIR"
log "Node.js version: $NODE_VERSION.x"

# Update package index
log "Updating package index..."
apk update >> "$LOG_FILE" 2>&1 || error "Failed to update package index"

# Install Node.js and npm (required for pk-puzldai)
log "Installing Node.js $NODE_VERSION and npm..."
apk add --no-cache \
    nodejs \
    npm \
    git \
    curl \
    wget \
    jq \
    bash \
    coreutils \
    procps \
    >> "$LOG_FILE" 2>&1 || error "Failed to install Node.js packages"

# Verify Node.js version
NODE_INSTALLED=$(node --version 2>/dev/null || echo "none")
log "Node.js version installed: $NODE_INSTALLED"

# Check if Node.js version is sufficient (>=20.0.0)
NODE_MAJOR=$(echo "$NODE_INSTALLED" | sed 's/v//' | cut -d. -f1)
if [ "$NODE_MAJOR" -lt 20 ] 2>/dev/null; then
    log "Warning: pk-puzldai requires Node.js 20+. Installed: $NODE_INSTALLED"
    log "Some features may not work correctly."
fi

# Create installation directory
log "Creating installation directory..."
mkdir -p "$INSTALL_DIR"
mkdir -p "$INSTALL_DIR/global"

# Configure npm to use local directory for global packages (no root required)
log "Configuring npm global directory..."
npm config set prefix "$INSTALL_DIR/global" >> "$LOG_FILE" 2>&1

# Install pk-puzldai - check for bundled version first
log "Installing pk-puzldai..."

PUZLDAI_INSTALLED=false

# Priority 1: Use bundled local build (from Android assets)
if [ -f "$BUNDLED_DIR/index.js" ]; then
    log "Found bundled pk-puzldai, installing from local build..."

    # Create package directory structure
    PACKAGE_DIR="$INSTALL_DIR/global/lib/node_modules/pk-puzldai"
    mkdir -p "$PACKAGE_DIR/dist/cli"

    # Copy bundled files
    cp "$BUNDLED_DIR/index.js" "$PACKAGE_DIR/dist/cli/index.js"
    chmod +x "$PACKAGE_DIR/dist/cli/index.js"

    # Copy package.json if available
    if [ -f "$BUNDLED_DIR/package.json" ]; then
        cp "$BUNDLED_DIR/package.json" "$PACKAGE_DIR/package.json"
        PUZLDAI_VERSION=$(cat "$BUNDLED_DIR/package.json" | grep '"version"' | head -1 | sed 's/.*"version": *"\([^"]*\)".*/\1/')
        log "Bundled version: $PUZLDAI_VERSION"
    else
        # Create minimal package.json
        cat > "$PACKAGE_DIR/package.json" << 'PKGJSON_EOF'
{
  "name": "pk-puzldai",
  "version": "0.3.0-bundled",
  "bin": {
    "pk-puzldai": "dist/cli/index.js"
  }
}
PKGJSON_EOF
    fi

    # Create bin symlink
    mkdir -p "$INSTALL_DIR/global/bin"
    ln -sf "$PACKAGE_DIR/dist/cli/index.js" "$INSTALL_DIR/global/bin/pk-puzldai"

    log "Installed pk-puzldai from bundled local build"
    PUZLDAI_INSTALLED=true
fi

# Priority 2: Try npm registry
if [ "$PUZLDAI_INSTALLED" = false ]; then
    log "No bundled version found, trying npm registry..."
    if npm install -g pk-puzldai >> "$LOG_FILE" 2>&1; then
        log "Installed pk-puzldai from npm registry"
        PUZLDAI_INSTALLED=true
    fi
fi

# Priority 3: Try GitHub
if [ "$PUZLDAI_INSTALLED" = false ]; then
    log "npm registry failed, trying GitHub..."
    if npm install -g github:kingkillery/Puzld.ai >> "$LOG_FILE" 2>&1; then
        log "Installed pk-puzldai from GitHub"
        PUZLDAI_INSTALLED=true
    fi
fi

# Fallback: Create error wrapper
if [ "$PUZLDAI_INSTALLED" = false ]; then
    log "All installation methods failed, creating fallback wrapper..."
    cat > "$INSTALL_DIR/pk-puzldai-fallback.js" << 'FALLBACK_EOF'
#!/usr/bin/env node
console.log('pk-puzldai TUI');
console.log('Installation incomplete - please install manually:');
console.log('  npm install -g pk-puzldai');
process.exit(1);
FALLBACK_EOF
    chmod +x "$INSTALL_DIR/pk-puzldai-fallback.js"
    log "Created fallback wrapper - manual install required"
fi

# Install additional useful tools
log "Installing additional CLI tools..."
npm install -g \
    @anthropic-ai/sdk \
    >> "$LOG_FILE" 2>&1 || log "Some optional npm packages failed, continuing..."

# Create wrapper script for easy invocation
log "Creating wrapper script..."
cat > /usr/local/bin/pk-puzldai << 'WRAPPER_EOF'
#!/bin/sh
# pk-puzldai wrapper script
export PATH="/opt/pk-puzldai/global/bin:$PATH"
export NODE_PATH="/opt/pk-puzldai/global/lib/node_modules:$NODE_PATH"

# Run pk-puzldai with all arguments
if command -v pk-puzldai >/dev/null 2>&1; then
    exec pk-puzldai "$@"
else
    # Try direct node execution
    if [ -f "/opt/pk-puzldai/global/lib/node_modules/pk-puzldai/dist/cli/index.js" ]; then
        exec node "/opt/pk-puzldai/global/lib/node_modules/pk-puzldai/dist/cli/index.js" "$@"
    else
        echo "pk-puzldai not found. Install with: npm install -g pk-puzldai"
        exit 1
    fi
fi
WRAPPER_EOF
chmod +x /usr/local/bin/pk-puzldai

# Create TUI launcher script
log "Creating TUI launcher script..."
cat > /usr/local/bin/pk-tui << 'TUI_EOF'
#!/bin/sh
# pk-puzldai TUI launcher
export PATH="/opt/pk-puzldai/global/bin:$PATH"
export NODE_PATH="/opt/pk-puzldai/global/lib/node_modules:$NODE_PATH"

# Launch TUI mode (no arguments = TUI)
exec pk-puzldai tui "$@" 2>/dev/null || exec pk-puzldai "$@"
TUI_EOF
chmod +x /usr/local/bin/pk-tui

# Create agent runner script for running arbitrary CLI tools as agents
log "Creating agent runner script..."
cat > /usr/local/bin/agent-run << 'AGENT_EOF'
#!/bin/sh
# Agent runner - executes commands with pk-puzldai orchestration
# Usage: agent-run <task_description>

export PATH="/opt/pk-puzldai/global/bin:$PATH"

if [ -z "$1" ]; then
    echo "Usage: agent-run <task_description>"
    echo "Example: agent-run 'list all files in current directory'"
    exit 1
fi

# Run task through pk-puzldai do command
exec pk-puzldai do "$*"
AGENT_EOF
chmod +x /usr/local/bin/agent-run

# Set up environment variables
log "Setting up environment..."
cat > /etc/profile.d/pk-puzldai.sh << 'ENV_EOF'
# pk-puzldai environment
export PATH="/opt/pk-puzldai/global/bin:/usr/local/bin:$PATH"
export NODE_PATH="/opt/pk-puzldai/global/lib/node_modules:$NODE_PATH"
export PK_PUZLDAI_HOME="/opt/pk-puzldai"
ENV_EOF

# Create user-level config directory
mkdir -p /home/udroid/.config/pk-puzldai
cat > /home/udroid/.config/pk-puzldai/config.json << 'CONFIG_EOF'
{
  "agents": {
    "default": "claude",
    "available": ["claude", "gemini", "ollama"]
  },
  "settings": {
    "theme": "dark",
    "showBanner": true,
    "approvalMode": "default"
  },
  "api": {
    "anthropic": null,
    "openai": null,
    "google": null
  }
}
CONFIG_EOF
chown -R udroid:udroid /home/udroid/.config/pk-puzldai

# Create marker file
touch "$MARKER_FILE"
chown udroid:udroid "$MARKER_FILE"

log "============================================"
log "pk-puzldai installation complete!"
log ""
log "Installed tools:"
log "  pk-puzldai        - Multi-LLM orchestration CLI"
log "  pk-tui            - Launch TUI mode directly"
log "  agent-run <task>  - Run a task with agents"
log ""
log "Quick start:"
log "  pk-puzldai              # Launch TUI"
log "  pk-puzldai do 'task'    # Execute a task"
log "  pk-puzldai chat         # Chat mode"
log "  pk-puzldai compare      # Compare agents"
log ""
log "Configuration:"
log "  /home/udroid/.config/pk-puzldai/config.json"
log ""
log "Set your API keys:"
log "  export ANTHROPIC_API_KEY='your-key'"
log "  export GOOGLE_API_KEY='your-key' (for gemini)"
log "============================================"
