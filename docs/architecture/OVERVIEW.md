# Architecture Overview

## System Architecture

```
                                    ┌─────────────────┐
                                    │    Frontend     │
                                    │   React + TS    │
                                    │   Port: 5173    │
                                    └────────┬────────┘
                                             │
                                             ▼
                                    ┌─────────────────┐
                                    │   API Gateway   │
                                    │  Spring Cloud   │
                                    │   Port: 10000   │
                                    └────────┬────────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        │                        │
                    ▼                        ▼                        ▼
           ┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
           │  Auth Service   │      │  Items Service  │      │  (Future Svc)   │
           │  Port: 9001     │      │  Port: 9000     │      │                 │
           └────────┬────────┘      └────────┬────────┘      └─────────────────┘
                    │                        │
                    ▼                        ▼
           ┌─────────────────┐      ┌─────────────────┐
           │  PostgreSQL     │      │  PostgreSQL     │
           │  Port: 5433     │      │  Port: 5432     │
           └─────────────────┘      └─────────────────┘

                              ┌─────────────────┐
                              │      Redis      │
                              │   Port: 6379    │
                              └─────────────────┘
                                (Used by Gateway
                                 for L2 caching)
```

## Service Responsibilities

### API Gateway (Port 10000)
- **Routing:** Route requests to appropriate backend services
- **CORS:** Centralized CORS handling for all services
- **Authentication:** Validate tokens before forwarding requests
- **Caching:** Two-layer cache (Caffeine L1 local + Redis L2 distributed)

### Auth Service (Port 9001)
- **Registration:** User signup with validation
- **Authentication:** Login with opaque token generation
- **Session Management:** Track active sessions
- **Token Refresh:** Issue new tokens before expiration
- **Health Checks:** Liveness and readiness probes via Actuator

### Items Service (Port 9000)
- **CRUD Operations:** Create, read, update, delete items

### Frontend (Port 5173)
- **Pages:** Home, Login, Register, Items Catalog, Item Detail
- **State:** Zustand for auth state, React Query for server state
- **Routing:** Protected routes requiring authentication
- **API Communication:** Axios with interceptors for auth headers

## Data Flow Examples

### User Login Flow

```
1. User submits credentials
   Frontend → POST /api/v1/auth/login → API Gateway

2. Gateway validates request format
   API Gateway → POST /auth/login → Auth Service

3. Auth Service validates credentials
   Auth Service → Query → PostgreSQL (auth)

4. On success, token returned
   Auth Service → Token → API Gateway → Frontend

5. Frontend stores token (via Zustand authStore)
   localStorage.setItem('onlineshop_auth', JSON.stringify({token, ...}))

6. Subsequent requests include token
   Headers: { Authorization: 'Bearer <token>' }
```

### Item Retrieval Flow (with Caching)

```
1. User requests items
   Frontend → GET /api/v1/items → API Gateway

2. Gateway checks L1 cache (Caffeine)
   Cache HIT → Return immediately

3. L1 miss, check L2 cache (Redis)
   Cache HIT → Store in L1 → Return

4. L2 miss, forward to service
   API Gateway → GET /items → Items Service

5. Items Service queries database
   Items Service → Query → PostgreSQL (items)

6. Response cached at both layers
   Response → Redis (L2) → Caffeine (L1) → Frontend
```

## Technology Stack

### Backend
| Component   | Technology           | Version  |
|-------------|----------------------|----------|
| Runtime     | Java                 | 25       |
| Framework   | Spring Boot          | 4.0.0    |
| Gateway     | Spring Cloud Gateway | 2025.1.0 |
| ORM         | Hibernate/JPA        | 7.0      |
| Database    | PostgreSQL           | 16       |
| Cache       | Redis                | 7        |
| Local Cache | Caffeine             | Latest   |
| Build       | Maven                | 3.9+     |

### Frontend
| Component     | Technology            | Version |
|---------------|-----------------------|---------|
| Framework     | React                 | 19      |
| Language      | TypeScript            | 5.9     |
| Build Tool    | Vite                  | 7.2     |
| CSS           | Tailwind CSS          | 4.1     |
| State         | Zustand               | 5.0     |
| Data Fetching | React Query           | 5.90    |
| HTTP Client   | Axios                 | 1.13    |
| Forms         | React Hook Form + Zod | Latest  |

### Infrastructure
| Component        | Technology                         |
|------------------|------------------------------------|
| Containerization | Docker                             |
| Orchestration    | Docker Compose (Kubernetes planned)|
| Databases        | PostgreSQL 16 (Alpine)             |
| Caching          | Redis 7 (Alpine)                   |

## Network Configuration

### Internal Docker Network
All services communicate via `onlineshop-network` bridge network.

### Service Discovery
Currently using Docker Compose service names:
- `api-gateway` resolves to API Gateway container
- `items-service` resolves to Items Service container
- `auth-service` resolves to Auth Service container
- `items-postgres` resolves to Items PostgreSQL container
- `auth-postgres` resolves to Auth PostgreSQL container
- `redis` resolves to Redis container

### CORS Configuration
Gateway allows origins:
- `http://localhost:5173` (Vite dev)
- `http://localhost:5174` (Vite alt)
- `http://localhost:3000` (CRA fallback)
- `http://127.0.0.1:5173`

## Scalability Considerations

### Current State
- All services MUST be able to run in multiple instances
- PostgreSQL per service (no shared DB)
- Redis shared for caching
