#!/usr/bin/env bash
# Runs once after the dev container is created.
# Idempotent: safe to re-run after a rebuild.
set -euo pipefail

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
# Install Claude Code CLI system-wide.
# We install with sudo into /usr/local (npm's default global prefix) rather
# than into a per-user .npm-global volume. Reinstall on rebuild is ~20s, not
# worth the volume management overhead.
# ---------------------------------------------------------------------------
echo "[post-create] Installing Claude Code CLI..."
if ! command -v claude >/dev/null 2>&1; then
  sudo npm install -g @anthropic-ai/claude-code || {
    echo "[post-create] WARN: claude CLI install failed — install manually with 'sudo npm i -g @anthropic-ai/claude-code'"
  }
else
  echo "[post-create] claude CLI already installed: $(claude --version 2>/dev/null || echo 'unknown')"
fi

echo "[post-create] Ensuring mvnw is executable..."
chmod +x Auth/mvnw Items/mvnw api-gateway/mvnw 2>/dev/null || true

# ---------------------------------------------------------------------------
# Pre-warm Maven dependency cache so the first VS Code "Run" click doesn't
# spend 5 min downloading. Runs the three services in parallel.
# ---------------------------------------------------------------------------
echo "[post-create] Pre-warming Maven dependency cache (one-time, ~3-5 min)..."
(cd Auth        && ./mvnw -q -T 1C -DskipTests dependency:go-offline) &
PID_AUTH=$!
(cd Items       && ./mvnw -q -T 1C -DskipTests dependency:go-offline) &
PID_ITEMS=$!
(cd api-gateway && ./mvnw -q -T 1C -DskipTests dependency:go-offline) &
PID_GW=$!

wait $PID_AUTH || echo "[post-create] WARN: Auth dependency resolution failed"
wait $PID_ITEMS || echo "[post-create] WARN: Items dependency resolution failed"
wait $PID_GW || echo "[post-create] WARN: api-gateway dependency resolution failed"

# ---------------------------------------------------------------------------
# Frontend deps.
# frontend/node_modules is a container-only named volume that shadows the
# host directory, so this always starts from an empty state and does a
# full Linux install — no Windows binary contamination.
# ---------------------------------------------------------------------------
if [ -d frontend ]; then
  echo "[post-create] Installing frontend dependencies..."
  (cd frontend && npm install) || echo "[post-create] WARN: frontend npm install failed"
fi

echo "[post-create] Done."
echo
echo "Next steps:"
echo "  - Click 'Run' in VS Code on AuthApplication / ItemsApplication / ApiGatewayApplication"
echo "  - Or run from a terminal:  cd Auth && ./mvnw spring-boot:run"
echo "  - Frontend:                  cd frontend && npm run dev -- --host 0.0.0.0"
echo "  - Headless Claude:           claude -p 'your prompt here'"
