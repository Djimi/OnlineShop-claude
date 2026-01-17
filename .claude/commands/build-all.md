Build all microservices respecting the Maven dependency graph:

**Phase 1 (Parallel - No dependencies):**
- Auth service
- common module
- api-gateway

**Phase 2 (After common completes):**
- Items service (depends on common)

Use background tasks for Phase 1, wait for common to complete, then build Items.
Report build status and any failures.