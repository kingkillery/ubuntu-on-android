#!/bin/sh
# Agent tools installation script for Alpine Linux (proot environment)
# This script installs pk-puzldai, factory.ai droid CLI, and dependencies for agent orchestration

set -e

INSTALL_DIR="/opt/agent-tools"
VENV_DIR="$INSTALL_DIR/venv"
MARKER_FILE="/home/udroid/.agent-tools-installed"
LOG_FILE="/tmp/agent-tools-install.log"

log() {
    echo "[pk-puzldai] $1" | tee -a "$LOG_FILE"
}

error() {
    echo "[pk-puzldai] ERROR: $1" | tee -a "$LOG_FILE"
    exit 1
}

# Check if already installed
if [ -f "$MARKER_FILE" ]; then
    log "Agent tools are already installed"
    log "To reinstall, remove $MARKER_FILE and run again"
    exit 0
fi

log "Starting agent tools installation..."
log "Install directory: $INSTALL_DIR"
log "This will install: pk-puzldai, factory.ai droid CLI"

# Update package index
log "Updating package index..."
apk update >> "$LOG_FILE" 2>&1 || error "Failed to update package index"

# Install Python and required packages
log "Installing Python and dependencies..."
apk add --no-cache \
    python3 \
    py3-pip \
    py3-virtualenv \
    git \
    curl \
    wget \
    jq \
    bash \
    coreutils \
    >> "$LOG_FILE" 2>&1 || error "Failed to install base packages"

# Install build dependencies for pip packages that need compilation
log "Installing build dependencies..."
apk add --no-cache \
    gcc \
    musl-dev \
    python3-dev \
    libffi-dev \
    openssl-dev \
    >> "$LOG_FILE" 2>&1 || log "Some build deps failed, continuing..."

# Create installation directory
log "Creating installation directory..."
mkdir -p "$INSTALL_DIR"

# Create Python virtual environment
log "Creating Python virtual environment..."
python3 -m venv "$VENV_DIR" >> "$LOG_FILE" 2>&1 || error "Failed to create venv"

# Activate virtual environment and upgrade pip
log "Upgrading pip in virtual environment..."
"$VENV_DIR/bin/pip" install --upgrade pip >> "$LOG_FILE" 2>&1 || error "Failed to upgrade pip"

# Install pk-puzldai from PyPI or GitHub
log "Installing pk-puzldai..."
# Try PyPI first, fall back to GitHub
if "$VENV_DIR/bin/pip" install pk-puzldai >> "$LOG_FILE" 2>&1; then
    log "Installed pk-puzldai from PyPI"
else
    log "PyPI install failed, trying GitHub..."
    if "$VENV_DIR/bin/pip" install git+https://github.com/puzlai/pk-puzldai.git >> "$LOG_FILE" 2>&1; then
        log "Installed pk-puzldai from GitHub"
    else
        log "GitHub install failed, trying alternative sources..."
        # Install anthropic and openai clients as fallback
        "$VENV_DIR/bin/pip" install anthropic openai httpx rich typer >> "$LOG_FILE" 2>&1 || error "Failed to install core deps"
        log "Installed core agent dependencies as fallback"
    fi
fi

# Install additional useful agent dependencies
log "Installing additional agent tools..."
"$VENV_DIR/bin/pip" install \
    anthropic \
    openai \
    httpx \
    rich \
    typer \
    pyyaml \
    python-dotenv \
    >> "$LOG_FILE" 2>&1 || log "Some optional deps failed, continuing..."

# Install factory.ai droid CLI
log "Installing factory.ai droid CLI..."
if "$VENV_DIR/bin/pip" install droid-cli >> "$LOG_FILE" 2>&1; then
    log "Installed droid-cli from PyPI"
else
    log "PyPI install failed for droid-cli, trying GitHub..."
    if "$VENV_DIR/bin/pip" install git+https://github.com/factory-ai/droid.git >> "$LOG_FILE" 2>&1; then
        log "Installed droid-cli from GitHub"
    else
        log "droid-cli installation failed, continuing without it..."
    fi
fi

# Install Google Gemini CLI
log "Installing Google Gemini CLI..."
if "$VENV_DIR/bin/pip" install google-generativeai >> "$LOG_FILE" 2>&1; then
    log "Installed google-generativeai from PyPI"
else
    log "google-generativeai installation failed, continuing..."
fi

# Install gemini-cli if available
if "$VENV_DIR/bin/pip" install gemini-cli >> "$LOG_FILE" 2>&1; then
    log "Installed gemini-cli from PyPI"
else
    log "gemini-cli not found in PyPI, creating wrapper..."
fi

# Create wrapper script for easy invocation
log "Creating wrapper script..."
cat > /usr/local/bin/pk-puzldai << 'WRAPPER_EOF'
#!/bin/sh
# pk-puzldai wrapper script
VENV_DIR="/opt/pk-puzldai/venv"

# Activate virtual environment
. "$VENV_DIR/bin/activate"

# Run pk-puzldai with all arguments
exec python3 -m pk_puzldai "$@" 2>/dev/null || exec python3 -c "
import sys
try:
    from pk_puzldai import main
    main()
except ImportError:
    print('pk-puzldai module not found, trying anthropic...')
    import anthropic
    print('Anthropic client available. Use: python3 -c \"import anthropic; ...\"')
" "$@"
WRAPPER_EOF
chmod +x /usr/local/bin/pk-puzldai

# Create droid wrapper script
log "Creating droid wrapper script..."
cat > /usr/local/bin/droid << 'DROID_WRAPPER_EOF'
#!/bin/sh
# factory.ai droid CLI wrapper script
VENV_DIR="/opt/agent-tools/venv"

# Activate virtual environment
. "$VENV_DIR/bin/activate"

# Run droid with all arguments
exec python3 -m droid "$@" 2>/dev/null || exec droid "$@" 2>/dev/null || {
    echo "droid CLI not found. Install with: pip install droid-cli"
    exit 1
}
DROID_WRAPPER_EOF
chmod +x /usr/local/bin/droid

# Create gemini wrapper script for Google search and AI queries
log "Creating gemini wrapper script..."
cat > /usr/local/bin/gemini << 'GEMINI_WRAPPER_EOF'
#!/bin/sh
# Google Gemini CLI wrapper script
# Usage: gemini "your question or search query"
VENV_DIR="/opt/agent-tools/venv"

# Activate virtual environment
. "$VENV_DIR/bin/activate"

# Try gemini-cli first, fall back to Python wrapper
exec python3 -m gemini_cli "$@" 2>/dev/null || {
    # Fallback: Use google-generativeai directly
    python3 << GEMINI_PYTHON
import os
import sys
import google.generativeai as genai

query = ' '.join(sys.argv[1:]) if len(sys.argv) > 1 else None

if not query:
    print("Usage: gemini 'your question'")
    print("Set GOOGLE_API_KEY or GEMINI_API_KEY environment variable")
    sys.exit(1)

api_key = os.environ.get('GEMINI_API_KEY') or os.environ.get('GOOGLE_API_KEY')
if not api_key:
    print("Error: Set GEMINI_API_KEY or GOOGLE_API_KEY environment variable")
    sys.exit(1)

try:
    genai.configure(api_key=api_key)
    model = genai.GenerativeModel('gemini-pro')
    response = model.generate_content(query)
    print(response.text)
except Exception as e:
    print(f"Error: {e}", file=sys.stderr)
    sys.exit(1)
GEMINI_PYTHON
}
GEMINI_WRAPPER_EOF
chmod +x /usr/local/bin/gemini

# Create agent runner script for running arbitrary CLI tools as agents
log "Creating agent runner script..."
cat > /usr/local/bin/agent-run << 'AGENT_EOF'
#!/bin/sh
# Agent runner - executes commands with agent orchestration
# Usage: agent-run <task_description>

VENV_DIR="/opt/pk-puzldai/venv"
. "$VENV_DIR/bin/activate"

TASK="$*"

if [ -z "$TASK" ]; then
    echo "Usage: agent-run <task_description>"
    echo "Example: agent-run 'list all files in current directory'"
    exit 1
fi

# Simple agent loop using available tools
python3 << PYTHON_EOF
import subprocess
import sys
import os

task = '''$TASK'''
print(f"[agent] Task: {task}")
print("[agent] Executing...")

# For now, just execute the task as a shell command if it looks like one
# In a full implementation, this would use an LLM to decompose tasks
try:
    result = subprocess.run(task, shell=True, capture_output=True, text=True, timeout=60)
    if result.stdout:
        print(result.stdout)
    if result.stderr:
        print(result.stderr, file=sys.stderr)
    sys.exit(result.returncode)
except subprocess.TimeoutExpired:
    print("[agent] Task timed out after 60 seconds", file=sys.stderr)
    sys.exit(124)
except Exception as e:
    print(f"[agent] Error: {e}", file=sys.stderr)
    sys.exit(1)
PYTHON_EOF
AGENT_EOF
chmod +x /usr/local/bin/agent-run

# Set up environment variables
log "Setting up environment..."
cat >> /etc/profile.d/agent-tools.sh << 'ENV_EOF'
# Agent tools environment (pk-puzldai, droid, etc.)
export PATH="/opt/agent-tools/venv/bin:/usr/local/bin:$PATH"
export AGENT_TOOLS_HOME="/opt/agent-tools"
export PK_PUZLDAI_HOME="/opt/agent-tools"
ENV_EOF

# Create user-level config
mkdir -p /home/udroid/.config/pk-puzldai
cat > /home/udroid/.config/pk-puzldai/config.yaml << 'CONFIG_EOF'
# pk-puzldai configuration
# Add your API keys here or via environment variables

# Anthropic API
# anthropic_api_key: "your-key-here"  # Or set ANTHROPIC_API_KEY env var

# OpenAI API (optional)
# openai_api_key: "your-key-here"  # Or set OPENAI_API_KEY env var

# Agent settings
agent:
  default_model: "claude-sonnet-4-20250514"
  max_tokens: 4096
  temperature: 0.7

# Tool settings
tools:
  shell: true
  filesystem: true
  network: true
CONFIG_EOF
chown -R udroid:udroid /home/udroid/.config/pk-puzldai

# Create marker file
touch "$MARKER_FILE"
chown udroid:udroid "$MARKER_FILE"

log "============================================"
log "Agent tools installation complete!"
log ""
log "Installed tools:"
log "  pk-puzldai          - pk-puzldai agent CLI"
log "  droid               - factory.ai droid CLI"
log "  gemini              - Google Gemini CLI"
log "  agent-run <task>    - Run a task with agent"
log ""
log "Configuration:"
log "  /home/udroid/.config/pk-puzldai/config.yaml"
log ""
log "Set your API keys:"
log "  export ANTHROPIC_API_KEY='your-key'"
log "  export OPENAI_API_KEY='your-key' (optional)"
log "  export GEMINI_API_KEY='your-key' (for gemini)"
log "============================================"
