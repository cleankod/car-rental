# Car Rental

Charles River Development / State Street technical assessment: a simulated car rental system. Given a
car type (Sedan, SUV, or Van), a desired start date/time, and a number of days, reserve one unit of
that type if the fleet has capacity — atomically, even under concurrent requests.

Full internal docs (architecture, request flow, testing strategy, ADRs) live in [`docs/`](docs/index.md)
as an MkDocs site — see [Docs site](#docs-site) below to run it locally. This README covers setup,
verification, and the assessment-specific write-ups (limitations, AI usage, given more time).

## Prerequisites

- Java 25 (the Gradle wrapper provisions it automatically if not already installed locally)
- Docker, only if you want to build/run the container image

## Quick start

```bash
./gradlew build
```

Run it:

```bash
./gradlew bootJar
java -jar build/libs/car-rental-0.0.1-SNAPSHOT.jar
```

Or in Docker (packages the already-built jar — run `./gradlew bootJar` first):

```bash
docker compose up --build
```

Try it:

```bash
curl -i -X POST http://localhost:8080/api/v1/reservations \
  -H "Content-Type: application/json" \
  -d '{"carType":"SEDAN","start":"2026-08-01T10:00:00","days":3}'
```

Fleet sizes (default: 2 Sedans, 2 SUVs, 1 Van) are configured in `src/main/resources/application.yml`
under `car-rental.fleet.*`.

## Running tests

```bash
./gradlew test
```

No Docker or Testcontainers needed — everything is in-memory, including the tests.

## Architecture

Lightweight Hexagonal Architecture (`domain` / `application` / `adapter` / `config`), evaluated against
plain layered rather than defaulted to. See [`docs/architecture.md`](docs/architecture.md) for the
package layout, request-flow, and domain-model diagrams, and
[ADR 0001](docs/decisions/0001-use-lightweight-hexagonal-architecture.md) for the full evaluation.

## Concurrency

`CarInventoryRepository.reserve(carType, period)` is one atomic method, not split into separate read/
write calls, so the concrete in-memory adapter can make the whole check-then-record step atomic — a
dedicated lock per `CarType`, so concurrent attempts for the same type serialize correctly while
different types never contend. Proven directly by racing many threads for the last available unit, not
just by code inspection. See [ADR 0002](docs/decisions/0002-use-per-car-type-locking-for-atomic-allocation.md).

## Testing strategy

Unit tests for pure domain logic (`RentalPeriod`, `Reservation`, `CarTypeInventory`); everything with an
injected collaborator is proven through the real REST endpoint instead, so internal refactoring doesn't
break tests as long as the HTTP behaviour is unchanged. Concurrency is the one deliberate exception —
see [`docs/testing.md`](docs/testing.md) and [ADR 0003](docs/decisions/0003-favor-black-box-tests-through-the-rest-entry-point.md).

## Known limitations and trade-offs

- **In-memory only** — reservations don't survive a restart, and per-car-type locking only guarantees
  correctness within a single JVM; a real deployment would need a shared store with real transactional
  or optimistic-locking semantics. Deliberate: the brief explicitly allows simulating persistence.
- **Conservative admission rule, not an optimal scheduler** — `CarTypeInventory` accepts a candidate iff
  fewer than `totalUnits` existing reservations overlap it; existing reservations are never reassigned
  between units. In rare fragmented-inventory cases, a period a cleverer reassignment could still fit
  may be rejected. Chosen for simplicity and predictability over optimality.
- **No time zone** — `RentalPeriod`/`ReservationRequest` use `LocalDateTime` throughout, which carries no
  offset or zone; two clients in different time zones sending the same `LocalDateTime` value mean
  different real instants, silently. Assumes a single implicit time zone shared by client and server. A
  real system would use `OffsetDateTime`/`Instant` plus an explicit zone policy.
- **Unbounded per-type reservation scan** — `InMemoryCarInventoryRepository` keeps every reservation ever
  accepted for a car type and scans all of them on each `reserve` call. Fine at this project's toy scale;
  a real deployment would narrow via an indexed DB range query (`WHERE car_type=? AND start<? AND
  end>?`) or an in-memory start-time-sorted structure bounded by a max rental duration. Neither requires
  changing `CarTypeInventory` or `Reservation` — the domain rule is narrowing-strategy-agnostic by design.
- **No cancellation or modification** of an existing reservation — the brief only asks for creating one.
- **Fleet sizes are fixed at startup** (`application.yml`), with no admin API to inspect or adjust them
  at runtime — considered mid-project and deferred as new scope (see "Given more time").
- **No past-date validation** — a reservation can be requested for a start date in the past; nothing in
  the domain currently depends on "the current moment," so there's no `Clock` abstraction either
  (deferred alongside the admin-API idea, for the same reason: not needed by anything currently in scope).
- **Coarser failure localization in tests** — since `ReservationService`/`InMemoryCarInventoryRepository`
  have no dedicated unit test, a bug there surfaces as a failing REST test, not a failing lower-layer
  one. Traded deliberately for refactorability (see ADR 0003).
- **No auth, rate limiting, pagination, observability stack (Actuator/Micrometer), or CI/CD** — out of
  scope for a two-hour take-home; none of these are what the brief grades.

## AI usage disclosure

This project was built with Claude Code (Anthropic). The initial prompt that started it already fixed a
substantial share of the design before any code existed: project identity and tech stack (Java 25,
Spring Boot, Gradle Groovy DSL), a recommended architecture pattern (lightweight Hexagonal) explicitly
flagged as a default to evaluate rather than accept mechanically, the exact non-trivial areas to focus
depth on (limited inventory, rental-period boundary semantics, overlap detection, avoiding overbooking,
atomic allocation under concurrency, domain/infrastructure separation, meaningful tests, documented
limitations — precisely what this project ended up focusing on), the ADR policy, and the stage-by-stage
branch/commit plan, plus a list of hard restrictions for the bootstrap step (no domain/feature code yet,
no premature branch creation, no unjustified dependencies, and more).

From there, all design and implementation decisions — architecture, domain model, concurrency strategy,
testing approach — were directed and reviewed by me at each step; every non-trivial decision is recorded as an
ADR under [`docs/decisions/`](docs/decisions/0001-use-lightweight-hexagonal-architecture.md) with the
alternatives considered and why. Each stage (domain model, use case, persistence, REST adapter, testing,
docs) was implemented on its own branch and reviewed before the next one started.

A few concrete instances of directing or overriding an initial AI proposal:

- Rejected an initial hand-rolled test double and a Mockito-based test in favor of testing against real
  in-memory collaborators — became the black-box testing approach in ADR 0003.
- Chose the REST request shape (`start` + `days`, not `start` + `end`) after being shown the trade-off.
- Selected lightweight Hexagonal Architecture over plain layered after reviewing the evaluation in
  ADR 0001.
- Deferred two proposed features (runtime fleet-admin endpoints, a `Clock`/`FakeClock` abstraction) as
  out of scope after discussing the trade-offs.

Every stage's build and tests were run and confirmed green before committing, the concurrency tests were
run repeatedly to rule out flakiness, and every command in this README was actually executed, not just
written down.

## Given more time

- Admin endpoints to inspect and adjust fleet state at runtime — needs a new domain rule first (what
  happens when shrinking a fleet below its currently active reservation count).
- A `Clock`/`FakeClock` abstraction plus a "can't reserve a car for a period in the past" rule.
- Persisting reservations to a real datastore, so a per-car-type lock could be replaced by a
  transactional or optimistic-locking scheme that works across multiple app instances.
- Cancelling or modifying an existing reservation.
- Full observability (metrics, tracing) and a CI pipeline.

## Docs site

```bash
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
mkdocs serve
```

Then open <http://127.0.0.1:8000>.
