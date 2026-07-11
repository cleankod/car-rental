# Progress — Car Rental

## Stages

| Branch | Content | Status |
|--------|---------|--------|
| `project-scaffolding` | Skeleton bootstrap (renamed to `eu.cleankod.carrental`/`CarRentalApplication`), `.clinerules` and memory bank filled in, Spring Boot 4.1.0, exploded-layers Dockerfile packaging a pre-built jar | ✅ merged (PR #1) |
| `architecture-boundaries` | Package skeleton (`domain`/`application.port.in`/`application.port.out`/`adapter.out.persistence`), ADR 0001 (lightweight Hexagonal vs. plain layered) | ✅ merged (PR #2) |
| `domain-model` | `CarType`, `RentalPeriod`, `ReservationId`/`Reservation`, `CarTypeInventory` (limited-inventory admission rule), `InvalidRentalPeriodException`/`InvalidFleetSizeException`, overlap/boundary-semantics unit tests | ✅ merged (PR #3) |
| `reservation-use-case` | `ReserveCarUseCase`, `ReservationService`, `CarInventoryRepository` port, and the real (not yet concurrency-safe) `InMemoryCarInventoryRepository` — tests run against that real repository, not a mock/fake | ✅ merged (PR #4) |
| `in-memory-persistence` | Hardened `InMemoryCarInventoryRepository` with per-car-type locking for atomic allocation, concurrency tests (races threads for the last unit), ADR 0002 | 🔄 in progress |
| `rest-api` | Optional minimal REST adapter — only if time remains | ⬜ planned |
| `documentation` | README, limitations/trade-offs, AI usage disclosure, "given more time" | ⬜ planned |

<!-- Status legend: ⬜ planned · 🔄 in progress · ✅ merged -->

## Out of Scope (documented as future improvements in README)

- Persisting reservations beyond process memory (a real database)
- Cancelling/modifying an existing reservation
- Pricing/billing
- Full observability stack (metrics, tracing)
- CI/CD pipeline
- Kubernetes / Helm
- Authentication / authorization
- Event sourcing / CQRS / sagas / messaging of any kind
