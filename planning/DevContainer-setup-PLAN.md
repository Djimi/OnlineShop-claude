# Dev Container Setup — PLAN

## Goal

Run the OnlineShop Spring Boot microservices (Auth, Items, api-gateway) and the React frontend from **inside** a VS Code Dev Container, with:

- VS Code "Run/Debug" button working for every service (Java extension, breakpoints, hot reload)
- Claude Code available **both** headlessly (CLI) and via the VS Code extension inside the container
- All infra (2× Postgres, Redis, Kafka, Kafka UI, pgAdmin) running in sibling containers — reused from the existing root `docker-compose.yml`
- All service and infra ports forwarded to the Windows host so Postman, DBeaver, pgAdmin-in-browser, etc. keep working exactly as before
- Minimal changes to existing service code: a small `application-devcontainer.yml` per service (required because `SPRING_DATASOURCE_URL` differs per service and can't be set globally via `remoteEnv`)

## Architecture decisions

- **`dockerComposeFile` override, not a standalone image.** The devcontainer extends the root `docker-compose.yml` and adds one `workspace` service. The workspace joins `onlineshop-network`, so app-to-infra hostnames become `auth-postgres`, `items-postgres`, `redis`, `kafka`.
- **`runServices` gates what Compose starts.** Only infra + workspace start. `auth-service` / `items-service` / `api-gateway` defined in root `docker-compose.yml` are **not** touched by the devcontainer (you run them from inside the workspace via VS Code's Run button or `./mvnw spring-boot:run`). Root compose remains untouched — `docker compose up` outside the devcontainer still works the same.
- **Spring profile `devcontainer`.** Activated via `SPRING_PROFILES_ACTIVE=devcontainer` in `remoteEnv`. Each service gets a tiny `application-devcontainer.yml` overriding hostnames only. VS Code Run button and `./mvnw spring-boot:run` both inherit the env → zero per-launch configuration needed.
- **Port forwarding strategy.** Infra ports stay mapped by the root `docker-compose.yml` (unchanged). Service ports (9000, 9001, 10000, 5173) are declared in `forwardPorts` on the workspace service so VS Code auto-binds them to the Windows host.
- **JDK 25.** Project uses Java 25; workspace image must match. Temurin 25 via the `java` devcontainer feature.
- **Node LTS** inside workspace for frontend dev server + Claude Code CLI.
- **No Docker socket mount.** Considered the `docker-outside-of-docker` feature for `docker ps`/`docker logs` quality-of-life, but mounting `/var/run/docker.sock` gives any in-container process (including a yolo-mode agent) effective root on the host. Use Windows PowerShell for those commands instead.
- **`~/.claude` persisted.** Bind-mount host `~/.claude` so Claude Code credentials, memory, and settings survive container rebuilds.

## Tasks

- [ ] Replace `.devcontainer/devcontainer.json` stub with compose-based config
- [ ] Create `.devcontainer/docker-compose.yml` (workspace service + infra-only `runServices`)
- [ ] Create `.devcontainer/Dockerfile` (JDK 25 base + Node + Claude CLI + tools)
- [ ] Create `Auth/src/main/resources/application-devcontainer.yml`
- [ ] Create `Items/src/main/resources/application-devcontainer.yml`
- [ ] Create `api-gateway/src/main/resources/application-devcontainer.yml`
- [ ] Verify `.vscode/launch.json` needs no changes (env inherited from container)
- [ ] Add `postCreateCommand` that pre-warms Maven deps (optional but saves 5 min on first run)
- [ ] Document "how to rebuild / how to run services" in the devcontainer README

## Files to create / modify

**Create:**
- `.devcontainer/docker-compose.yml`
- `.devcontainer/Dockerfile`
- `.devcontainer/README.md`
- `Auth/src/main/resources/application-devcontainer.yml`
- `Items/src/main/resources/application-devcontainer.yml`
- `api-gateway/src/main/resources/application-devcontainer.yml`

**Modify:**
- `.devcontainer/devcontainer.json` (full rewrite of the stub)

**Do NOT touch:**
- Root `docker-compose.yml` (stays fully usable as-is for non-devcontainer workflows)
- `.vscode/launch.json` (inherits `SPRING_PROFILES_ACTIVE` from container env)
- Service `application.yml` files (profile override only, no changes to base config)

## Known issues / open items

- [ ] **CORS origins in api-gateway include only localhost.** Frontend inside container serves at `http://localhost:5173` from the user's browser perspective (via port forwarding), so CORS still matches. No change needed — verify at smoke test.
- [ ] **Kafka advertised listeners.** Root `docker-compose.yml` advertises `kafka:9092` (container network) and `localhost:29092` (host). From inside the workspace container, apps should use `kafka:9092` — handled in profile. Verify no service currently has hardcoded `29092`.
- [ ] **First-run Maven download.** Cold dev container spends ~5 min pulling dependencies. Mitigate with a named volume for `~/.m2` in the workspace service.
- [ ] **Windows line endings / file perms.** `mvnw` must stay executable; `core.autocrlf` can corrupt it. Verify `./mvnw` runs cleanly inside the container; if not, `git config core.autocrlf input` or `.gitattributes` fix.
- [ ] **Claude Code CLI install.** Install in image via `npm i -g @anthropic-ai/claude-code` (verify package name at build time).
- [ ] **`~/.claude` bind-mount on Windows.** Path is `${localEnv:USERPROFILE}/.claude`. Confirm Windows-host mount semantics work.
