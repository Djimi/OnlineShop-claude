# Architectural Patterns

## Design Patterns

Standard design patterns (Repository, Factory, Strategy, etc.) are applied where appropriate throughout the codebase.

## Anti-Patterns to Avoid

### Distributed Monolith
❌ Microservices that are tightly coupled and must be deployed together.

✅ Services that can be developed, deployed, and scaled independently.

### Shared Database
❌ Multiple services accessing the same database tables.

✅ Each service owns its data, communicates via APIs or events.
