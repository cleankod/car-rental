# Active Context — Car Rental

## Current State

- Branch: `in-memory-persistence`
- Based on: `master` after `reservation-use-case` was merged (PR #4)
- Task: hardening the *same* `InMemoryCarInventoryRepository` for concurrency (per-car-type locking),
  adding concurrency tests, and ADR 0002. No new repository class was created — this stage upgrades the
  one already built in `reservation-use-case`.

## Most Recent Decisions

- Concurrency strategy: a dedicated lock object per configured `CarType` (not the enum constant itself —
  avoids synchronizing on a value other code could also lock on), with each type's reservation list also
  stored separately (`EnumMap<CarType, List<Reservation>>`). `reserve` synchronizes on the requested
  type's lock for the whole check-then-record step. Chosen over a single global lock (would needlessly
  serialize unrelated car types) and over lock-free/CAS (too complex to justify for this scope). Full
  write-up in `docs/decisions/0002-use-per-car-type-locking-for-atomic-allocation.md`.
- Concurrency tests race many threads (`ExecutorService` + `CountDownLatch` to maximize contention)
  against the same car type and assert the exact success/rejection counts — one test with a single unit
  (simplest case), one with `totalUnits = 3` and more attempts than capacity (stronger, more general
  proof that the count-based rule holds under concurrency, not just the trivial single-unit case). Ran
  the suite 5 times to rule out flakiness before committing.
- `CarInventoryRepository` exposes one method, `reserve(carType, period)`, deliberately shaped as a
  single atomic operation rather than split into separate read/write methods — splitting it would make
  true atomicity impossible for a later implementation to retrofit, since the check-then-act gap would
  already be baked into the port contract.
- `ReservationService` is a thin delegate to the port — correct, not under-implemented: the atomic
  decision belongs to the repository, so there's no orchestration logic left for the service to own at
  this stage's scope (one use case, one port).
- Tests exercise `ReservationService`/`InMemoryCarInventoryRepository` directly against each other — not
  mocked. Codified in `.clinerules` Testing rules: prefer real, dependency-free collaborators over
  Mockito.
- Exception design: each exception builds its own message from raw constructor values, and maps to
  exactly one invariant — codified in `.clinerules` Domain exception rules.
- `var` used only when the type is apparent from the right-hand side — codified in `.clinerules` Code
  style rules.
- Tests use JUnit 5 `@Nested` classes grouped by method/behaviour under test, plain-sentence method names
  inside — not method-name prefixes.
- Selected lightweight Hexagonal Architecture over plain layered — evaluated per `.clinerules`
  "Architecture rules", not defaulted; full rationale in
  `docs/decisions/0001-use-lightweight-hexagonal-architecture.md`.
- Persistence: in-memory only, no DB/migrations, now concurrency-safe per car type.
- No messaging, no secondary/mocked integrations — single synchronous use case.
- No Testcontainers, Actuator, Micrometer, or MkDocs — none apply to this assignment's scope.
- REST adapter is optional, deferred to its own later stage; the brief only requires unit tests.

## Branching Workflow

1. User creates branch, tells Claude Code.
2. Claude Code implements with individual commits.
3. User pushes, reviews, may request fixes.
4. User merges to `master`, pulls, tells Claude Code.
5. Claude Code acknowledges, suggests next branch name.
6. Repeat.

## Project Status

Bootstrap, rules, architectural boundaries, the domain model, and the reservation use case are merged.
Currently on `in-memory-persistence`, adding per-car-type locking, concurrency tests, and ADR 0002 to the
existing `InMemoryCarInventoryRepository`. Next stage after this one is the optional `rest-api`, or
straight to `documentation` if that's skipped. **Branch creation is the user's step, not Claude Code's**
— implementation of the next stage begins only after the user creates and checks out that branch and
confirms.

## Session Resumption Notes

- When resuming, read `progress.md` for overall status and this file for recent decisions.
- Always check `git branch --show-current` and `git status` before making changes.
- Do not start the next branch until the user confirms `master` is in the expected state and tells
  Claude Code which branch to work on.
- Build command: `./gradlew build`
- Test command: `./gradlew test`
- No Docker/Testcontainers dependency for tests — plain `./gradlew test` is sufficient.
