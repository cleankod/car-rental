# Progress — Car Rental

## Stages

| Branch | Content | Status |
|--------|---------|--------|
| `project-scaffolding` | Skeleton bootstrap (renamed to `eu.cleankod.carrental`/`CarRentalApplication`), `.clinerules` and memory bank filled in, Spring Boot 4.1.0, exploded-layers Dockerfile packaging a pre-built jar | ✅ merged (PR #1) |
| `architecture-boundaries` | Package skeleton (`domain`/`application.port.in`/`application.port.out`/`adapter.out.persistence`), ADR 0001 (lightweight Hexagonal vs. plain layered) | 🔄 in progress |
| `domain-model` | `CarType`, `RentalPeriod`, `Reservation`, domain exceptions, overlap/boundary semantics + unit tests | ⬜ planned |
| `reservation-use-case` | `ReserveCarUseCase`, `ReservationService`, `CarInventoryRepository` port + unit tests against a fake repository | ⬜ planned |
| `in-memory-persistence` | Thread-safe in-memory `CarInventoryRepository`, atomic allocation, concurrency tests, concurrency-strategy ADR | ⬜ planned |
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
