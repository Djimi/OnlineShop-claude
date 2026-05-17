# DDD Items Improvement Plan

> Consolidated from 3 sub-agent investigations: Items service codebase audit, modern DDD best practices (2024-2025), and Spring Boot DDD implementation patterns.

---

## Current State Assessment

**Overall DDD Maturity Score: 5.5 / 10**

The service has solid DDD scaffolding — proper aggregate roots, value objects, domain events, repository ports, CQRS-lite command/query separation — but lacks domain depth. The event pipeline is dead code, the domain model is thin, and there are architectural violations.

| Category | Score | Key Gap |
|----------|-------|---------|
| Aggregate Design | 7/10 | Proper factory/reconstitution methods; slightly thin on business behavior |
| Value Objects | 7/10 | Clean records with validation; minor null-inconsistency, wrong package |
| Domain Events | 4/10 | Good event classes; **zero listeners** — entire pipeline is dead code |
| Repository Pattern | 9/10 | Clean domain interface + adapter; proper JPA separation with mapper |
| Domain Services | 5/10 | Only `IdGenerator` (technical, not business domain service) |
| Application Services | 7/10 | Clean use case pattern; web exception leaking, commands as web DTOs |
| ACL / Bounded Contexts | 5/10 | Item VOs in `common` library leak across bounded contexts |
| Testing | 2/10 | Single test file, no domain tests, no integration tests |

---

## Improvement Tasks

### Phase 1: Critical Fixes (Bug-level)

- [x] **1.1 — Fix `@PathVariable id` bug in `PUT /{id}`**
  - **File:** `ItemsController.java` lines 66-68
  - **Problem:** `@PathVariable UUID id` is received but never used. The `UpdateItemCommand` carries its own `UUID id` from the JSON body. A client could send `PUT /items/aaa-bbb` with body `{"id": "ccc-ddd", ...}` and item `ccc-ddd` would be updated instead of `aaa-bbb`.
  - **Fix:** Validate that `command.id().equals(id)` and return `400 Bad Request` via `ResponseStatusException` if they differ.

- [x] **1.2 — Add missing `@DeleteMapping` endpoint**
  - **File:** `ItemsController.java`
  - **Problem:** `DeleteItemUseCase` and `DeleteItemCommand` exist and are fully implemented, but there is no `@DeleteMapping` in the controller.
  - **Fix:** Added `@DeleteMapping("/{id}")` method constructing a `DeleteItemCommand(id)` from the path variable and calling `deleteItemUseCase.execute()`, returning `204 No Content`.

- [x] **1.3 — Move `ItemNotFoundException` out of `web.exception`**
  - **Files:** `web/exception/ItemNotFoundException.java`, `UpdateItemUseCase.java`, `DeleteItemUseCase.java`, `GetItemUseCase.java`, `GlobalExceptionHandler.java`
  - **Problem:** Application use cases import and throw `web.exception.ItemNotFoundException` — a reverse dependency (`application` → `web`). In DDD, the dependency arrow must point inward.
  - **Fix:** Created `domain/exception/ItemNotFoundException.java`. Removed the `web.exception` one. Updated `GlobalExceptionHandler` to map the new exception to HTTP 404. Also added `ResponseStatusException` handler for the new 400 validation.

### Phase 2: Architecture Cleanup

- [ ] **2.1 — Implement domain event handler(s)**
  - **Problem:** Domain events are published via `ApplicationEventPublisher` in every command use case, but there is not a single `@EventListener` or `@TransactionalEventListener` anywhere.
  - **Fix:** Add at least one listener (e.g., structured logging listener via `@TransactionalEventListener(phase = AFTER_COMMIT)`) to prove the pipeline works. This validates the entire flow: `AggregateRoot.registerEvent()` → `events.clear()` → `ApplicationEventPublisher.publishEvent()`.

- [ ] **2.2 — Create separate web DTOs for requests**
  - **Files:** `ItemsController.java`, command classes in `application/command/`
  - **Problem:** `CreateItemCommand` and `UpdateItemCommand` are used directly as `@RequestBody` in the controller. Web DTOs (serialization concerns, validation annotations, OpenAPI docs) should be separate from application commands (use case semantics).
  - **Fix:** Create `web/dto/CreateItemRequest.java` and `web/dto/UpdateItemRequest.java`. Controller accepts these, maps to application commands in a private mapper method.

- [ ] **2.3 — Move Item-specific value objects from `common` to `Items`**
  - **Files:** `common/.../valueobject/ItemId.java`, `ItemName.java`, `ItemDescription.java`, `Quantity.java`
  - **Problem:** The `common` library contains Items-bounded-context-specific value objects. Other services depending on `common` would transitively receive these — a bounded context violation.
  - **Fix:** Relocate VOs to `Items/src/main/java/com/onlineshop/items/domain/valueobject/`. Keep only `BaseId`, `BaseEntity`, `AggregateRoot`, `DomainEvent`, `BaseDomainEvent` in `common`.

- [ ] **2.4 — Extract `toResponse()` mappers from use cases**
  - **Files:** All use case classes in `application/usecase/`
  - **Problem:** Use cases mix orchestration logic with DTO mapping logic (`private toResponse()` methods). This makes mapping hard to test and violates SRP.
  - **Fix:** Extract mapping to dedicated mapper classes (e.g., `application/dto/mapper/ItemResponseMapper.java`).

### Phase 3: Domain Depth

- [ ] **3.1 — Seal domain event hierarchy**
  - **Files:** `domain/event/ItemCreated.java`, `ItemUpdated.java`, `ItemDeleted.java`
  - **Problem:** Domain events extend `BaseDomainEvent` but are not part of a sealed hierarchy. A future developer adding `ItemPriceChanged` could miss updating all handlers.
  - **Fix:** Add `sealed interface ItemDomainEvent extends DomainEvent permits ItemCreated, ItemUpdated, ItemDeleted`. Update event classes to implement `ItemDomainEvent`.

- [ ] **3.2 — Fix `ItemDescription` null handling**
  - **Files:** `ItemDescription.java`, all use case `toResponse()` methods
  - **Problem:** `ItemName` rejects null; `ItemDescription` accepts it. Forces null checks like `item.getDescription() != null ? item.getDescription().value() : null` in 4 different `toResponse()` methods.
  - **Fix:** Make `ItemDescription` non-nullable with empty string as default. Remove scattered null checks.

- [ ] **3.3 — Enrich `Item` aggregate with business behavior**
  - **File:** `domain/aggregateroots/Item.java`
  - **Problem:** The aggregate only has `updateDetails()` (simple setter with event) and `markAsDeleted()`. `Quantity.isGreaterThanZero()` is never called by the aggregate. Missing real domain logic.
  - **Fix:** Add stock management methods:
    - `increaseStock(Quantity amount)` — adds stock
    - `decreaseStock(Quantity amount)` — reduces stock with invariant: cannot go below zero
    - `reserveStock(Quantity amount)` — reserves stock (if future orders context)

- [ ] **3.4 — Remove unused `@Setter` import from `BaseEntity.java`**
  - **File:** `common/.../entity/BaseEntity.java` line 7
  - **Fix:** Remove `import lombok.Setter;`

### Phase 4: Testing

- [ ] **4.1 — Add domain unit tests**
  - Test `Item` aggregate behavior: factory creation, state transitions (`updateDetails`, `markAsDeleted`), event registration
  - Test value object validation: `ItemName`, `ItemDescription`, `Quantity` boundary cases
  - Test domain event structure
  - **No Spring context** — pure JUnit 5 + AssertJ

- [ ] **4.2 — Add use case integration tests**
  - Test `CreateItemUseCase`, `UpdateItemUseCase`, `DeleteItemUseCase`
  - Use `@DataJpaTest` with in-memory DB or Testcontainers (PostgreSQL parity)
  - Verify events are published and persisted correctly

- [ ] **4.3 — Add controller integration tests**
  - Test `ItemsController` endpoints with `@WebMvcTest`
  - Verify HTTP status codes, response bodies, and error handling (404, 400)

---

## Issues (Post-Implementation Tracking)

| # | Issue | Status |
|---|-------|--------|
| 1 | **No Transactional Outbox** — Events published via `ApplicationEventPublisher` with no delivery guarantee. Lost on process crash before broker gets them. | 🔴 Deferred (needs Spring Modulith or custom Outbox table) |
| 2 | **`@Data` on `BaseId` generates setters** — Minor risk for value object immutability | 🟡 Low priority |
| 3 | **No Integration Event translation layer** — Domain events carry VO payloads directly. Publishing to external broker would leak internal model. | 🔴 Deferred (needed when adding real message broker) |
| 4 | **Queries hydrate full aggregates** — `GetAllItemsUseCase`, `SearchItemsUseCase` load complete `Item` aggregates even for summaries. Could optimize with `JdbcTemplate` or read model projection. | 🟡 Low priority (deferred until performance issue) |
