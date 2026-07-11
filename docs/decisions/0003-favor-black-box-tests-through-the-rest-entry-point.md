# 0003. Favor black-box tests through the REST entry point

## Status

Accepted

## Context

Once the REST adapter existed, several lower-layer tests became redundant: `ReservationServiceTest`
(2 methods) and `InMemoryCarInventoryRepositoryTest` (5 methods) tested scenarios that
`ReservationControllerTest` already proves end-to-end, through the exact same real objects — this
codebase never mocks; every collaborator in every test is the genuine implementation. Both classes have
an injected collaborator (`ReservationService` takes `CarInventoryRepository`; the repository is stateful,
adapter-like infrastructure), which raised the question of whether they should keep dedicated,
per-layer tests at all now that a real end-to-end entry point exists.

A sibling assessment repository (`bet-settlement-trigger`) had already answered this question for an
analogous shape of project. Its rule, verbatim: *"Prefer integration tests over internal mocking. Unit
tests only for pure logic with no injected dependencies."* In practice that produced exactly one
dependency-free domain unit test and one full-stack HTTP integration test — no separate
application-service or repository test — with its own ADR stating the same rationale this decision
adopts: *"This tolerates internal refactoring without test breakage."*

## Decision Drivers

- Redundant tests across layers add maintenance cost without adding confidence: a change to
  `ReservationService`'s internals would require updating both its own test and the REST test, even
  though the REST test alone already proves the observable behaviour is correct.
- Refactorability: internal restructuring of `ReservationService` or `InMemoryCarInventoryRepository`
  (e.g. merging the service into the controller, changing the repository's locking strategy) should not
  break tests as long as the HTTP contract is unchanged.
- The brief explicitly asks for unit tests — this must not be read as license to remove unit tests from
  the layer where they genuinely earn their keep: the pure domain rules (`RentalPeriod`,
  `CarTypeInventory`) that this system's correctness actually rests on.
- Concurrency-safety is a named non-trivial requirement that cannot be reliably proven through real HTTP
  calls — racing `TestRestTemplate` requests would be slow and non-deterministic, and couldn't reproduce
  the tight `CountDownLatch`-synchronized contention needed to reliably hit the contended case.

## Considered Options

1. Keep dedicated unit tests at every layer (domain, application service, persistence adapter, REST).
2. Collapse to pure-domain unit tests plus one REST-level integration test proving the wiring, with
   concurrency kept as a deliberate white-box exception (selected).
3. Drop the REST-level test and keep only per-layer unit tests (mirror the pre-REST-adapter test shape).

## Advantages and Disadvantages

### Option 1 — unit tests at every layer

- (+) Fast failure localization: a broken test points precisely at the layer that broke.
- (-) The same scenario (e.g. "rejected when no unit remains") ends up asserted three times
  (`ReservationServiceTest`, `InMemoryCarInventoryRepositoryTest`, `ReservationControllerTest`) against
  the exact same real objects — pure duplication, not independent coverage.
- (-) Refactoring `ReservationService` or the repository's internals — even when the REST contract stays
  identical — breaks tests that were never actually protecting anything the REST test doesn't already
  protect.

### Option 2 — pure-domain unit tests + one REST black-box test (selected)

- (+) Every class with an injected dependency is proven through the same path a real caller would use —
  the REST endpoint — so its test doesn't need to change when the class's internals are refactored.
- (+) No duplication: each scenario is asserted once, at the layer that actually owns it (the business
  *rule* in `CarTypeInventoryTest`/`RentalPeriodTest`, the end-to-end *wiring* in
  `ReservationControllerTest`).
- (+) Matches a working precedent (`bet-settlement-trigger`) already validated for a project of this
  shape.
- (-) Losing a scenario during the collapse is a real risk if not audited carefully — see "Consequences"
  for how this was verified.
- (-) A failing REST test doesn't localize *which* internal component broke as precisely as a per-layer
  test would.

### Option 3 — REST test only where it doesn't overlap, otherwise per-layer

- (+) Avoids fully committing to either extreme.
- (-) In practice this becomes an ad hoc judgment call per scenario with no clear rule to apply
  consistently, unlike option 2's clean line (dependency-free → unit test; has an injected
  dependency → REST test).

## Decision

Unit-test only pure, dependency-free classes directly: `RentalPeriod`, `Reservation`, `CarTypeInventory`,
and the domain exceptions. Every class with an injected collaborator — `ReservationService`,
`InMemoryCarInventoryRepository` — is proven exclusively through the real REST entry point
(`ReservationControllerTest`, full Spring context, real objects, no mocks), not in its own isolated test.
`InMemoryCarInventoryRepositoryConcurrencyTest` remains a deliberate, named exception: it races threads
directly against `repository.reserve(...)` because that specific property cannot be reliably proven
through HTTP.

## Rationale

The dividing line is not "which package is it in" but "does this class have an injected dependency."
Pure value objects/rules are where a fast, precise unit test earns its keep — and where you'd actually
want a tight TDD loop if the business rule itself changes. Anything wired through Spring is better
proven through the stable, observable contract a real caller depends on (the HTTP API), so that
internal refactoring doesn't require touching tests that were never protecting anything beyond what the
REST test already covers. This mirrors `bet-settlement-trigger`'s validated approach for an analogous
project shape, adapted to this project's own boundaries (no messaging/DB, so no Testcontainers
overhead — the Spring context here is lightweight, an embedded Tomcat only).

## Consequences

- Deleted `application/ReservationServiceTest.java` (2 methods) and
  `adapter/out/persistence/InMemoryCarInventoryRepositoryTest.java` (5 methods).
- Every scenario from the deleted files was cross-checked against what remains before deletion — nothing
  was silently dropped:
  - Success and rejection-when-unavailable scenarios were already fully covered by
    `ReservationControllerTest`'s success/conflict tests (same real objects, one layer up).
  - The `CarUnavailableException` message content (previously only asserted in the deleted files) is now
    asserted black-box, via `ErrorResponse.message()` in the REST conflict test.
  - The REST conflict test's second request was changed from an exact duplicate of the first to a
    genuinely overlapping-but-different period — otherwise the test would still pass even if overlap
    detection were broken and only object-equality worked, and the deleted repository test's distinct
    "overlapping but not identical" scenario would have had no equivalent left.
  - Two scenarios had no equivalent anywhere else and were added as new black-box tests in
    `ReservationControllerTest`: back-to-back (touching, non-overlapping) reservations of the same type
    both succeeding even at one unit, and capacity being tracked independently per car type.
- `ReservationControllerTest` is a single `@SpringBootTest` with a cached Spring context and no
  `@DirtiesContext`, so every test method's car-type/period combination must be chosen to avoid
  collisions with any other method regardless of JUnit's (unguaranteed) execution order — done here by
  giving each scenario a clearly separated month-scale time window.

## Known Limitations / Risks

- Failure localization is coarser for the application/persistence layers: a broken
  `InMemoryCarInventoryRepository` now surfaces as a failing REST test, not a failing repository test.
  Judged acceptable given the repository's logic is thin (delegates the actual decision to
  `CarTypeInventory.hasCapacityFor`, which remains directly unit-tested).
- Integration-level proof now depends on booting a Spring context per test class, which is slower than a
  plain unit test — though with no Testcontainers/DB in this project, this stays fast (an embedded
  Tomcat only), unlike `bet-settlement-trigger`'s ~15s Testcontainers-backed suite.
- Test-method independence in `ReservationControllerTest` relies on careful, manual choice of
  non-colliding time windows rather than a structural guarantee (e.g. `@DirtiesContext` per method, which
  was deliberately not used since it would slow the suite down for no correctness benefit at this scale).
  This is a manageable but real convention to maintain as more tests are added to that class.
