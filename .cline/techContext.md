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
| Testing | JUnit 5, AssertJ, (Mockito only where a fake genuinely clarifies a unit test) |
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
      rest                -- optional, minimal — only if the REST stage is implemented
    out
      persistence          -- thread-safe in-memory CarInventoryRepository implementation
  config                   -- Spring @Configuration / bean wiring, if any is needed
```

## Domain Model

- `CarType` — enum: `SEDAN`, `SUV`, `VAN`.
- `RentalPeriod` — record: `start` (`LocalDateTime`), `days` (`int`). Half-open interval
  `[start, start + days)`; `overlaps(other)` uses strict `isBefore` comparisons so two periods that only
  touch at the boundary (one ends exactly when the other starts) do **not** overlap — back-to-back
  reservations of the same car are allowed. Rejects `days <= 0` and a `null` start via
  `InvalidRentalPeriodException`.
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

<!-- Not yet implemented — lands in the reservation-use-case stage. -->

**Inbound:**
- `ReserveCarUseCase` — the one use case; called by tests and, if built, the REST controller

**Outbound:**
- `CarInventoryRepository` → thread-safe in-memory implementation, using `CarTypeInventory.hasCapacityFor`
  against whatever reservations it currently holds for that type

## Key Decisions

- Lightweight Hexagonal over plain layered — see "Architecture" above and `.clinerules`.
- In-memory persistence only, no DB/migrations — assignment is explicitly simulated and nothing needs to
  survive a restart.
- No messaging — single synchronous operation.
- No Testcontainers, Actuator, Micrometer, or MkDocs — none serve a purpose for this assignment's scope
  (see "Technology stack" in `.clinerules` for why).
- REST adapter is optional and deferred to its own stage — the brief only requires unit tests.
- Dockerfile does not build the jar — it expects `build/libs/*.jar` to already exist (run
  `./gradlew bootJar` on the host/CI first), mirroring a real CI's separate compile-then-package stages;
  `.dockerignore` sends only that jar into the build context. It then extracts the boot jar into Spring
  Boot's layers (`dependencies`/`spring-boot-loader`/`snapshot-dependencies`/`application`) instead of
  copying the fat jar as one layer, so code-only changes don't invalidate the cached dependency layer;
  runs as a non-root user. Plain `jar` task disabled in `build.gradle` so the Dockerfile's wildcard jar
  `COPY` stays unambiguous. See "Docker rules" in `.clinerules` — a Docker-construction detail, not a
  stack decision, so no ADR.
