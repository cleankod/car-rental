# Technical Context — Car Rental

## Tech Stack

| Concern | Technology |
|---------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 4 |
| Build | Gradle Groovy DSL + version catalog (`gradle/libs.versions.toml`) |
| Persistence | Plain in-memory collections, thread-safe (no DB — simulated system, nothing needs to survive a restart) |
| Migrations | None — no schema |
| Messaging | None — single synchronous operation, not event/queue-driven |
| Testing | JUnit 5, AssertJ — black-box by default: unit-test only pure/dependency-free classes; anything with an injected collaborator is proven through the real REST entry point, not mocked (see ADR 0003) |
| Observability | SLF4J + Logback only — Actuator/Micrometer deliberately dropped, no long-running service to monitor |
| Docs | MkDocs (Material theme) + Mermaid diagrams, `docs/` |

## Architecture

Chosen pattern: **lightweight Hexagonal Architecture**, package-by-feature (collapsed to root-level
since there is one feature). Evaluated against plain layered, not defaulted to — full rationale in
`docs/decisions/0001-use-lightweight-hexagonal-architecture.md`.

```
eu.cleankod.carrental
  domain                  -- Car, CarType, Reservation, RentalPeriod, domain exceptions
  application
    port
      in                  -- ReserveCarUseCase
      out                 -- CarInventoryRepository
    (service impl)        -- ReservationService
  adapter
    in
      rest                -- ReservationController, request/response DTOs, RestExceptionHandler
    out
      persistence          -- InMemoryCarInventoryRepository (concurrency-safe: per-car-type locking)
  config                   -- CarRentalConfiguration, FleetProperties (Spring bean wiring)
```

## Domain Model

- `CarType` — enum: `SEDAN`, `SUV`, `VAN`.
- `RentalPeriod` — record: `start` (`LocalDateTime`), `days` (`int`). Half-open interval
  `[start, start + days)`; `overlaps(other)` uses strict `isBefore` comparisons so two periods that only
  touch at the boundary (one ends exactly when the other starts) do **not** overlap — back-to-back
  reservations of the same car are allowed. Rejects `days <= 0` via `InvalidRentalPeriodException` (which
  builds its own message from `start`/`days`, not a message passed in); a `null` start is a precondition,
  rejected via `Objects.requireNonNull`, not a domain exception.
- `ReservationId` — record wrapping a `UUID`; `generate()` factory.
- `Reservation` — record: `id`, `carType`, `period`; `of(carType, period)` factory generates the id;
  `overlaps(other)` is true only when both the car type matches and the periods overlap.
- `CarTypeInventory` — record: `carType`, `totalUnits`. `hasCapacityFor(existingPeriods, candidate)`
  accepts the candidate iff fewer than `totalUnits` existing periods overlap it. Deliberately a
  conservative admission rule, not an optimal bin-repacking scheduler: existing reservations are never
  reassigned between units, so a small number of pathological/fragmented-inventory cases could reject a
  period that a cleverer reassignment could still fit — documented as a known trade-off, not an
  oversight. Rejects `totalUnits <= 0` via `InvalidFleetSizeException`.
- Exceptions so far: `InvalidRentalPeriodException`, `InvalidFleetSizeException`. `CarUnavailableException`
  (rejecting a reservation attempt) is deferred to the `reservation-use-case` stage, where something
  actually throws it — no point creating it earlier as dead code.

## Application Ports / Services

**Inbound:**
- `ReserveCarUseCase` — the one use case; called by tests and by `ReservationController`.
- `ReservationService` implements it as a thin delegate to `CarInventoryRepository` — it has no logic of
  its own beyond wiring, since the atomic check-and-record decision belongs entirely to the repository.

**Outbound:**
- `CarInventoryRepository` — a single method, `reserve(carType, period)`, deliberately atomic-shaped
  rather than split into separate read/write methods: splitting it would make true atomicity impossible
  for a later implementation to add, since the check-then-act gap would already be baked into the port
  contract.
- `InMemoryCarInventoryRepository` (`adapter.out.persistence`) — the real (not test-double) implementation:
  holds a `CarTypeInventory`, a reservation list, and a dedicated lock object per `CarType` (all fixed at
  construction). `reserve` synchronizes on the requested type's lock for the whole check-then-record
  step, so concurrent attempts for the *same* type are serialized (no overbooking) while attempts for
  *different* types never contend. See
  `docs/decisions/0002-use-per-car-type-locking-for-atomic-allocation.md` for the alternatives considered
  (single global lock, lock-free/CAS) and why per-type locking was chosen.
- Neither has its own dedicated unit test — both have an injected collaborator, so per ADR 0003 they're
  proven through `ReservationControllerTest` instead. `InMemoryCarInventoryRepositoryConcurrencyTest` is
  the one exception (see ADR 0003).

## REST Adapter

- `POST /api/v1/reservations` — `{carType, start, days}` → `201 Created` with the reservation. Request
  shape and error-handling mapping are recorded in `.clinerules` "REST API rules" — not repeated here.
- `CarRentalConfiguration`/`FleetProperties` wire the beans and externalize per-type unit counts from
  `application.yml` instead of hardcoding them.
- Tested via `ReservationControllerTest` (real Spring context, `TestRestTemplate`, no mocks) — see
  `.clinerules` "Testing rules" and ADR 0003 for the setup and the reasoning.
- Manually verified with the app actually running (`./gradlew bootJar` + `java -jar` + `curl`): success
  (201), capacity exhaustion (409), validation failure (400), and malformed enum value (400) all return
  the expected status and `ErrorResponse` shape.

## Build & Deploy

- Dockerfile packages a pre-built jar (doesn't compile it — see `.clinerules` "Docker rules") and
  extracts it into Spring Boot's layers for better layer-cache reuse on code-only changes.
- Internal docs are MkDocs (Material theme, Mermaid diagrams) under `docs/` — see `mkdocs.yml`.
