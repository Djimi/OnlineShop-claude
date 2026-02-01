# Plan: Product Info endpoint (api-gateway)

## Requirements recap
- Add public endpoint in `api-gateway` at `GET /api/product-info`
- No authentication/authorization
- No calls to other services (static response)
- Response payload must include:
  ```json
  { "description": "This is a test product for learning" }
  ```
- Follow `docs/API_DESIGN.md`; flag any deviations
- Update documentation chain (project + service docs + referenced docs)

## Implementation tasks
- [x] Inspect existing `api-gateway` routing/controllers to decide where to add a static endpoint.
- [x] Implement `GET /api/product-info` handler with static JSON response.
- [x] Ensure response formatting aligns with `docs/API_DESIGN.md` (camelCase, consistent JSON structure).
- [x] Add/adjust tests for the new endpoint (unit/web test as appropriate for gateway).
- [x] Update documentation tree:
  - [x] Root `AGENTS.md` if project-wide docs change
  - [x] Any docs referenced by `AGENTS.md` that are affected
  - [x] All files referenced by those docs (recursive)
  - [x] Service-level `AGENTS.md` files for all microservices

## Open issues / risks
- ✅ **API versioning mismatch**: Documented a gateway exception for public info endpoints in `docs/API_DESIGN.md` and kept `/api/product-info` as requested.
- ✅ **Response envelope**: Existing services return direct DTOs, so the endpoint responds with a plain JSON object and is documented as consistent with current conventions.
