# Implementation Details: Product Info endpoint

## Decisions
- Implemented `GET /api/product-info` as a gateway-local, static response with no downstream calls.
- Returned a plain JSON object with `description` only to match the requested payload and existing service responses.
- Documented an explicit exception to path versioning in `docs/API_DESIGN.md` for gateway-owned informational endpoints.

## Notes
- Endpoint is public and bypasses authentication because the auth filter only guards `/items/**`.
- Added a Spring Boot integration test using `HttpClient` to validate status, content type, and payload.
- Disabled rate limiting in the test via `gateway.ratelimit.enabled=false` to avoid Redis dependency during startup.
