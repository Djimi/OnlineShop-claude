# Debug Info

> **Important:** When making code changes to backend services, you MUST:
> 1. Build the JAR with Maven from the service directory (`./mvnw clean package`)
> 2. Stop the running service (`docker compose down <service-name>`)
> 3. Rebuild the Docker image from root (`docker compose up -d --build <service-name>`)
>
> Without step 1, the Docker image will use the old JAR and changes won't apply.


## Essential Commands

> **Note:** All `docker compose` commands must be run from the root project directory.

```bash
# Start all services
docker compose up -d

# Stop all services
docker compose down

# Stop all services AND remove volumes (DATABASE DATA WILL BE LOST)
# DANGER: Only run when explicitly requested and confirmed by user
docker compose down -v

# Apply code changes to a service (MUST build JAR first, then rebuild container)
# Run from service directory (e.g., Items/, Auth/, api-gateway/, etc):
./mvnw clean package -DskipTests
# Then rebuild the Docker image:
docker compose up -d --build <service-name>

# Run unit + integration tests (from service directory)
./mvnw clean test

# Run application with local profile without docker containers.
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Run e2e tests (from e2e-tests/, requires docker compose up first)
cd e2e-tests && ./mvnw clean test

# Frontend development
cd frontend && npm run dev

# Frontend build
cd frontend && npm run build
```
> **Important:** If you ran into an issue that container is existing, check whether it was created by that docker compose file or not. If not, you need to remove it manually using `docker rm <container-id>` command.
