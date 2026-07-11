# Active Context — Car Rental

## Current State

- Branch: `reservation-use-case`
- Based on: `master` after `domain-model` was merged (PR #3)
- Task: implementing `ReserveCarUseCase`, `ReservationService`, the `CarInventoryRepository` port, and
  the real (not yet concurrency-safe) `InMemoryCarInventoryRepository`. Concurrency-hardening of that
  same repository is deferred to `in-memory-persistence`.

## Most Recent Decisions

- `CarInventoryRepository` exposes one method, `reserve(carType, period)`, deliberately shaped as a
  single atomic operation rather than split into separate read/write methods — splitting it would make
  true atomicity impossible for a later implementation to retrofit, since the check-then-act gap would
  already be baked into the port contract.
- `ReservationService` is a thin delegate to the port — correct, not under-implemented: the atomic
  decision belongs to the repository, so there's no orchestration logic left for the service to own at
  this stage's scope (one use case, one port).
- Built the real `InMemoryCarInventoryRepository` now instead of a throwaway test fake, and tested
  `ReservationService`/`InMemoryCarInventoryRepository` directly against each other — not mocked. Avoids
  a hand-rolled double duplicating logic the real adapter already has; the next stage hardens this same
  class for concurrency rather than replacing it. Codified in `.clinerules` Testing rules: prefer real,
  dependency-free collaborators over Mockito.
- Exception design tightened: `InvalidRentalPeriodException`/`InvalidFleetSizeException`/
  `CarUnavailableException` all build their own message from raw constructor values (no pre-built
  message strings), and each maps to exactly one invariant — codified in `.clinerules` Domain exception
  rules.
- `var` used only when the type is apparent from the right-hand side (`new X(...)`, `catchThrowable(...)`,
  `hasXxxFor(...)`) — explicit type otherwise. Codified in `.clinerules` Code style rules.
- Tests use JUnit 5 `@Nested` classes grouped by method/behaviour under test, plain-sentence method names
  inside — not method-name prefixes.
- Selected lightweight Hexagonal Architecture over plain layered — evaluated per `.clinerules`
  "Architecture rules", not defaulted; full rationale in
  `docs/decisions/0001-use-lightweight-hexagonal-architecture.md`.
- Persistence: in-memory only, no DB/migrations; concurrency-safety is the next stage's job, not this
  one's.
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

Bootstrap, rules, architectural boundaries, and the domain model are merged. Currently on
`reservation-use-case`, implementing the application ports/service and the real in-memory repository.
Next stage after this one is `in-memory-persistence` (concurrency-hardening the same repository).
**Branch creation is the user's step, not Claude Code's** — implementation of the next stage begins only
after the user creates and checks out that branch and confirms.

## Session Resumption Notes

- When resuming, read `progress.md` for overall status and this file for recent decisions.
- Always check `git branch --show-current` and `git status` before making changes.
- Do not start the next branch until the user confirms `master` is in the expected state and tells
  Claude Code which branch to work on.
- Build command: `./gradlew build`
- Test command: `./gradlew test`
- No Docker/Testcontainers dependency for tests — plain `./gradlew test` is sufficient.
