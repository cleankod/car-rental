# Active Context — Car Rental

## Current State

- Branch: `black-box-testing`
- Based on: `master` after `rest-api` was merged (PR #6)
- Task: restructuring the test suite toward black-box coverage now that the REST adapter exists —
  deleting redundant per-layer tests, adding/strengthening REST-level scenarios, ADR 0003. Next stage
  after this one is `documentation`, the last planned stage.

## Most Recent Decisions

- Deleted `application/ReservationServiceTest.java` and
  `adapter/out/persistence/InMemoryCarInventoryRepositoryTest.java` entirely. Both test classes with an
  injected collaborator, whose scenarios are already fully proven end-to-end by
  `ReservationControllerTest` hitting the same real objects one layer up — modeled directly on
  `~/workspace/playground/bet-settlement-trigger`'s validated rule: "unit tests only for pure logic with
  no injected dependencies." Every deleted scenario was cross-checked against what remains before
  deletion; nothing was silently dropped. Full write-up in
  `docs/decisions/0003-favor-black-box-tests-through-the-rest-entry-point.md`.
- Two genuinely unique scenarios (no equivalent anywhere else) moved up to `ReservationControllerTest`
  as new black-box tests: `allowsBackToBackReservationsOfTheSameTypeWhenPeriodsDoNotOverlap` and
  `tracksCapacityIndependentlyAcrossCarTypes`.
- Strengthened the existing conflict test (renamed
  `returnsConflictWhenAnOverlappingPeriodLeavesNoUnitAvailable`): the second request now uses a
  genuinely overlapping-but-different period instead of an exact duplicate of the first (proves overlap
  detection itself is reachable through HTTP, not just repeat-request equality), and asserts
  `ErrorResponse.message()` contains "SUV" — closing a real coverage gap the deletion would otherwise
  have left (the `CarUnavailableException` message had no other test home).
  `InMemoryCarInventoryRepositoryConcurrencyTest` is untouched — the one deliberate white-box exception,
  since racing real HTTP calls can't reproduce the tight thread contention that test needs.
- Considered and declined two related ideas raised mid-stage: admin endpoints to add/inspect fleet state
  at runtime (deferred — real new scope: `CarTypeInventory.totalUnits` is currently immutable, making it
  runtime-adjustable needs a new domain rule for shrinking a fleet below its active-reservation count;
  not needed to solve test isolation, which is handled by non-colliding time windows instead); and a
  `Clock`/`FakeClock` bean (skipped — nothing in the domain currently depends on "now" — `RentalPeriod.start`
  is always caller-supplied — so it wouldn't change any production behavior, and it wouldn't fix the
  test-isolation issue either, which comes from the shared cached Spring context, not date-literal
  strategy; revisit only if a time-dependent rule like "can't reserve in the past" is added).
- REST request shape is `{carType, start, days}` (mirrors `RentalPeriod` directly) rather than
  `{carType, start, end}` — considered and decided against the date-range alternative: it would require
  inventing a policy for non-whole-day gaps (reject? round and silently shift the return time?), which is
  tangential to what this assignment grades, and the brief's own wording ("for a given number of days")
  already matches the `days` shape.
- `RestExceptionHandler` maps `CarUnavailableException`→409, `InvalidRentalPeriodException`→400, Bean
  Validation/malformed-JSON failures→400, anything else→500 generic — all as
  `ErrorResponse(errorId, code, message, details)`, never a raw exception name or stack trace.
- Fleet sizes externalized via `FleetProperties` (`@ConfigurationProperties`) and `application.yml`
  (`car-rental.fleet.*`) rather than hardcoded in `CarRentalConfiguration`.
- Hit a real dependency gap: Spring Boot 4 split `TestRestTemplate` out of `spring-boot-test` into a new
  `org.springframework.boot:spring-boot-resttestclient` module (package
  `org.springframework.boot.resttestclient`), which itself needs `org.springframework.boot:spring-boot-restclient`
  for `RestTemplateBuilder`, plus `@AutoConfigureTestRestTemplate` on the test class. Confirmed against
  `~/workspace/playground/bet-settlement-trigger`, which hit and solved the same issue.
- Concurrency strategy (from `in-memory-persistence`): a dedicated lock object per configured `CarType`,
  with each type's reservation list stored separately. Full write-up in
  `docs/decisions/0002-use-per-car-type-locking-for-atomic-allocation.md`.
- Selected lightweight Hexagonal Architecture over plain layered — evaluated per `.clinerules`
  "Architecture rules", not defaulted; full rationale in
  `docs/decisions/0001-use-lightweight-hexagonal-architecture.md`.
- No messaging, no secondary/mocked integrations. No Testcontainers, Actuator, Micrometer, or MkDocs.

## Branching Workflow

1. User creates branch, tells Claude Code.
2. Claude Code implements with individual commits.
3. User pushes, reviews, may request fixes.
4. User merges to `master`, pulls, tells Claude Code.
5. Claude Code acknowledges, suggests next branch name.
6. Repeat.

## Project Status

Bootstrap, rules, architectural boundaries, the domain model, the reservation use case, concurrency
hardening, and the REST adapter are all merged. Currently on `black-box-testing`, restructuring the test
suite. Next stage after this one is `documentation` (README, limitations/trade-offs, AI usage
disclosure, "given more time") — the last planned stage. **Branch creation is the user's step, not
Claude Code's** — implementation of the next stage begins only after the user creates and checks out
that branch and confirms.

## Session Resumption Notes

- When resuming, read `progress.md` for overall status and this file for recent decisions.
- Always check `git branch --show-current` and `git status` before making changes.
- Do not start the next branch until the user confirms `master` is in the expected state and tells
  Claude Code which branch to work on.
- Build command: `./gradlew build`
- Test command: `./gradlew test`
- No Docker/Testcontainers dependency for tests — plain `./gradlew test` is sufficient.
