# Active Context — Car Rental

## Current State

- Branch: `domain-model`
- Based on: `master` after `architecture-boundaries` was merged (PR #2)
- Task: implementing the domain model — `CarType`, `RentalPeriod`, `Reservation`, `CarTypeInventory`,
  domain exceptions, and their unit tests. No application/port/adapter behaviour yet — that starts in
  `reservation-use-case`.

## Most Recent Decisions

- Domain model implemented as immutable records: `CarType` (enum), `RentalPeriod` (half-open interval,
  `overlaps` deliberately excludes the touching-boundary case — back-to-back reservations allowed),
  `ReservationId`/`Reservation`, `CarTypeInventory` (the limited-inventory admission rule:
  `hasCapacityFor` accepts a candidate iff fewer than `totalUnits` existing periods overlap it).
- `CarTypeInventory`'s admission rule is a deliberate, documented trade-off: it's conservative (never
  overbooks) but not an optimal bin-repacking scheduler — existing reservations are never reassigned
  between units. Flagged as a known limitation for the README, not hidden as an oversight.
- `InvalidRentalPeriodException` and `InvalidFleetSizeException` created now since something throws them
  in this stage; `CarUnavailableException` deferred to `reservation-use-case`, where it's actually thrown
  — avoids dead/untested exception classes.
- Tests use JUnit 5 `@Nested` classes grouped by method/behaviour under test (e.g. `Constructor`,
  `Overlaps`, `HasCapacityFor`), plain-sentence method names inside — not method-name prefixes. Now
  codified in `.clinerules` Testing rules for later stages to follow.
- Selected lightweight Hexagonal Architecture over plain layered — evaluated per `.clinerules`
  "Architecture rules", not defaulted; full rationale in
  `docs/decisions/0001-use-lightweight-hexagonal-architecture.md`.
- Persistence: in-memory only, thread-safe, no DB/migrations.
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

Bootstrap, rules, and architectural boundaries are merged. Currently on `domain-model`, implementing the
domain types and their unit tests. Next stage after this one is `reservation-use-case`. **Branch
creation is the user's step, not Claude Code's** — implementation of the next stage begins only after
the user creates and checks out that branch and confirms.

## Session Resumption Notes

- When resuming, read `progress.md` for overall status and this file for recent decisions.
- Always check `git branch --show-current` and `git status` before making changes.
- Do not start the next branch until the user confirms `master` is in the expected state and tells
  Claude Code which branch to work on.
- Build command: `./gradlew build`
- Test command: `./gradlew test`
- No Docker/Testcontainers dependency for tests — plain `./gradlew test` is sufficient.
