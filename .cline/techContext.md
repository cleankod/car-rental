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
| Docs | Plain README (no MkDocs — disproportionate for a 2-hour take-home) |

## Architecture

Chosen pattern: **lightweight Hexagonal Architecture**, package-by-feature (collapsed to root-level
since there is one feature), evaluated against plain layered per the criteria in `.clinerules` —
not defaulted to. Why: the classic Hexagonal trigger (multiple real integrations, one mocked/swappable)
doesn't strictly hold — there is exactly one real boundary (in-memory storage). What justifies it anyway:
a named `CarInventoryRepository` port + one in-memory adapter costs almost nothing more than a plain
interface + implementation, and buys a clean separation between the reservation business rule and the
concurrency-control mechanism that enforces it — both explicitly named as areas to demonstrate depth on
in the brief. Kept deliberately lightweight: no adapter subtree for messaging/secondary integrations that
don't exist here, no multi-`UseCase` ceremony beyond the single real use case. Full rationale recorded in
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
- `ReservationService` and `InMemoryCarInventoryRepository` are no longer given their own dedicated unit
  tests — both have an injected collaborator, so per ADR 0003 they're proven exclusively through
  `ReservationControllerTest` (the real REST entry point), not in isolation. The one deliberate
  exception is `InMemoryCarInventoryRepositoryConcurrencyTest`, which races many threads directly
  against `repository.reserve(...)` for the same car type and asserts exactly `totalUnits` succeed and
  the rest are rejected — a property that can't be reliably reproduced by racing real HTTP calls.

## REST Adapter

- `POST /api/v1/reservations` — `{carType, start, days}` → `201 Created` with the reservation
  (`id`, `carType`, `start`, `days`). Mirrors `RentalPeriod`'s own shape rather than a `start`/`end`
  range — see "REST API rules" in `.clinerules` for the trade-off considered.
- `RestExceptionHandler` (`@RestControllerAdvice`) maps `CarUnavailableException` → `409`,
  `InvalidRentalPeriodException` → `400`, Bean Validation / malformed-JSON failures → `400`, anything
  else → `500` generic — all as `ErrorResponse(errorId, code, message, details)`, never a raw exception
  name or stack trace.
- `CarRentalConfiguration` wires `InMemoryCarInventoryRepository` and `ReservationService` as beans;
  `FleetProperties` (`@ConfigurationProperties(prefix = "car-rental.fleet")`) externalizes the per-type
  unit counts from `application.yml` instead of hardcoding them in the wiring.
- Tests: `ReservationControllerTest` — `@SpringBootTest(webEnvironment = RANDOM_PORT)` +
  `TestRestTemplate` against the real context (no `MockMvc`, no mocked use case). Needed two extra test
  dependencies Spring Boot 4 split out of the core test starter:
  `org.springframework.boot:spring-boot-restclient` (`RestTemplateBuilder`) and
  `org.springframework.boot:spring-boot-resttestclient` (`TestRestTemplate`, package
  `org.springframework.boot.resttestclient`) — also needs `@AutoConfigureTestRestTemplate` on the test
  class. Confirmed against a baseline playground project, which hit the same issue.
- Manually verified with the app actually running (`./gradlew bootJar` + `java -jar` + `curl`): success
  (201), capacity exhaustion (409), validation failure (400), and malformed enum value (400) all return
  the expected status and `ErrorResponse` shape.
- `ReservationControllerTest` now also covers (added in the `black-box-testing` stage, ADR 0003): the
  conflict scenario uses a genuinely overlapping-but-different second period (not an exact duplicate)
  and asserts the exception message, not just the error code; back-to-back (touching, non-overlapping)
  reservations of the same type both succeeding even at one unit; and capacity being tracked
  independently per car type. Test methods use clearly-separated month-scale time windows per scenario
  since the Spring context (and its singleton repository bean) is cached across all methods in the
  class with no `@DirtiesContext`, and JUnit doesn't guarantee method order.

## Key Decisions

- Black-box testing through the real REST entry point for anything with an injected collaborator
  (`ReservationService`, `InMemoryCarInventoryRepository`), unit tests only for pure/dependency-free
  domain classes, concurrency kept as a deliberate white-box exception — see ADR 0003. Modeled on a
  baseline playground project's validated precedent for an analogous project shape. Deleted
  `ReservationServiceTest` and `InMemoryCarInventoryRepositoryTest` after auditing every scenario against
  what remains — nothing was silently dropped (two genuinely unique scenarios moved up to
  `ReservationControllerTest` as new black-box tests).
- Per-car-type locking (dedicated lock object per `CarType`, not the enum constant itself) for atomic
  allocation, over a single global lock or lock-free/CAS — see ADR 0002.
- Lightweight Hexagonal over plain layered — see "Architecture" above and `.clinerules`.
- In-memory persistence only, no DB/migrations — assignment is explicitly simulated and nothing needs to
  survive a restart.
- No messaging — single synchronous operation.
- No Testcontainers, Actuator, Micrometer, or MkDocs — none serve a purpose for this assignment's scope
  (see "Technology stack" in `.clinerules` for why).
- REST adapter implemented (minimal, one endpoint) — the brief only requires unit tests, so this stays a
  demonstration of the adapter boundary, not a feature to expand. `start`+`days` request shape chosen
  over `start`+`end` deliberately — see "REST Adapter" above.
- Dockerfile does not build the jar — it expects `build/libs/*.jar` to already exist (run
  `./gradlew bootJar` on the host/CI first), mirroring a real CI's separate compile-then-package stages;
  `.dockerignore` sends only that jar into the build context. It then extracts the boot jar into Spring
  Boot's layers (`dependencies`/`spring-boot-loader`/`snapshot-dependencies`/`application`) instead of
  copying the fat jar as one layer, so code-only changes don't invalidate the cached dependency layer;
  runs as a non-root user. Plain `jar` task disabled in `build.gradle` so the Dockerfile's wildcard jar
  `COPY` stays unambiguous. See "Docker rules" in `.clinerules` — a Docker-construction detail, not a
  stack decision, so no ADR.
