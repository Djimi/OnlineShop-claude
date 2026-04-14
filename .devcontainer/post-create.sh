#!/usr/bin/env bash
# Runs once after the dev container is created.
# Idempotent: safe to re-run after a rebuild.
set -euo pipefail

# ---------------------------------------------------------------------------
# Load nvm so that `node` and `npm` resolve to the nvm-managed versions.
# The devcontainer node feature installs nvm but non-interactive scripts
# don't source ~/.bashrc, so we load it explicitly.
#
# Belt-and-suspenders: unset NPM_CONFIG_PREFIX in case a base image or
# feature left it set — nvm refuses to work when it's present.
# ---------------------------------------------------------------------------
unset NPM_CONFIG_PREFIX 2>/dev/null || true
export NVM_DIR="${NVM_DIR:-/usr/local/share/nvm}"
if [ -s "$NVM_DIR/nvm.sh" ]; then
  . "$NVM_DIR/nvm.sh"
fi

# ---------------------------------------------------------------------------
# Fix ownership on named volumes.
#
# Docker initializes fresh named volumes as root-owned. We mount two volumes
# into the vscode user's home (uid 1000):
#   - onlineshop-m2                     -> /home/vscode/.m2
#   - onlineshop-frontend-node-modules  -> <repo>/frontend/node_modules
# Without this chown, Maven wrapper fails with AccessDeniedException and
# npm install fails with EACCES. Safe to re-run: no-op once owned.
# ---------------------------------------------------------------------------
echo "[post-create] Fixing ownership on named volumes..."
sudo chown -R vscode:vscode /home/vscode/.m2 || true
sudo chown -R vscode:vscode /workspaces/OnlineShop-claude/frontend/node_modules || true

# ---------------------------------------------------------------------------
# Install Claude Code CLI via nvm-managed npm (no sudo).
#
# Using nvm's npm (not sudo npm) ensures the binary lands in nvm's bin
# directory which is already on the user's PATH. `sudo npm install -g`
# would use root's npm, putting the binary somewhere the vscode user's
# shell can't find.
# ---------------------------------------------------------------------------
echo "[post-create] Installing Claude Code CLI..."
if ! command -v claude >/dev/null 2>&1; then
  npm install -g @anthropic-ai/claude-code || {
    echo "[post-create] WARN: claude CLI install failed — install manually with 'npm i -g @anthropic-ai/claude-code'"
  }
else
  echo "[post-create] claude CLI already installed: $(claude --version 2>/dev/null || echo 'unknown')"
fi

echo "[post-create] Ensuring mvnw is executable..."
chmod +x Auth/mvnw Items/mvnw api-gateway/mvnw common/mvnw 2>/dev/null || true

echo "[post-create] Done."
echo
echo "First-time setup (only needed once — cached in named volumes):"
echo "  1. cd Auth        && ./mvnw -DskipTests compile"
echo "  2. cd Items       && ./mvnw -DskipTests compile"
echo "  3. cd api-gateway && ./mvnw -DskipTests compile"
echo "  4. cd frontend    && npm install"
echo
echo "After that:"
echo "  - Click 'Run' in VS Code on AuthApplication / ItemsApplication / ApiGatewayApplication"
echo "  - Or run from a terminal:  cd Auth && ./mvnw spring-boot:run"
echo "  - Frontend:                  cd frontend && npm run dev -- --host 0.0.0.0"
echo "  - Headless Claude:           claude -p 'your prompt here'"
