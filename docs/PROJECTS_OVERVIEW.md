# Project Overview

**Type:** Microservices-based e-commerce learning platform
**Purpose:** Learn technologies, architectural patterns, and UI development
**Owner:** Senior Java Developer learning frontend and advanced patterns

## Services & Ports

| Service       | Port  | Tech Stack                             | Description                               |
|---------------|-------|----------------------------------------|-------------------------------------------|
| API Gateway   | 10000 | Spring Cloud Gateway, Redis, Caffeine  | Central routing, caching, auth validation |
| Auth Service  | 9001  | Spring Boot 4, PostgreSQL              | Authentication, JWT tokens, sessions      |
| Items Service | 9000  | Spring Boot 4, PostgreSQL              | Product inventory CRUD                    |
| Frontend      | 5173  | React 19, TypeScript, Vite, Tailwind   | User interface                            |

