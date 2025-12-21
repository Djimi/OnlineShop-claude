# Architectural Patterns

## Anti-Patterns to Avoid

### Distributed Monolith
❌ Microservices that are tightly coupled and must be deployed together.

✅ Services that can be developed, deployed, and scaled independently.

### Shared Database
❌ Multiple services accessing the same database tables.

✅ Each service owns its data, communicates via APIs or events.
