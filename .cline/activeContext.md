# Active Context — Car Rental

## Current State

- Branch: `rest-api`
- Based on: `master` after `in-memory-persistence` was merged (PR #5)
- Task: implementing the minimal REST adapter — `ReservationController`, request/response DTOs,
  `RestExceptionHandler`, and the `config` package (`CarRentalConfiguration`/`FleetProperties`) wiring it
  all together. This is the optional stage; next after this is `documentation`.

## Most Recent Decisions

- REST request shape is `{carType, start, days}` (mirrors `RentalPeriod` directly) rather than
  `{carType, start, end}` — considered and decided against the date-range alternative: it would require
  inventing a policy for non-whole-day gaps (reject? round and silently shift the return time?), which is
  tangential to what this assignment grades, and the brief's own wording ("for a given number of days")
  already matches the `days` shape.
- `RestExceptionHandler` maps `CarUnavailableException`→409, `InvalidRentalPeriodException`→400, Bean
  Validation/malformed-JSON failures→400, anything else→500 generic — all as
  `ErrorResponse(errorId, code, message, details)`, never a raw exception name or stack trace.
- Fleet sizes externalized via `FleetProperties` (`@ConfigurationProperties`) and `application.yml`
  (`car-rental.fleet.*`) rather than hardcoded in `CarRentalConfiguration` — gives the previously-empty
  `config` package a real purpose now that something needs Spring wiring.
- `ReservationControllerTest` uses `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`
  against the real app context — no `MockMvc`, no mocked use case, consistent with the "prefer real
  collaborators" testing philosophy from earlier stages. Fleet sizes pinned to 1 unit/type via
  `@TestPropertySource` so capacity-exhaustion tests don't depend on production defaults.
- Hit a real dependency gap: Spring Boot 4 split `TestRestTemplate` out of `spring-boot-test` into a new
  `org.springframework.boot:spring-boot-resttestclient` module (package
  `org.springframework.boot.resttestclient`), which itself needs `org.springframework.boot:spring-boot-restclient`
  for `RestTemplateBuilder`, plus `@AutoConfigureTestRestTemplate` on the test class. Confirmed against
  `~/workspace/playground/bet-settlement-trigger`, which hit and solved the same issue.
- Manually verified the running app for real (`./gradlew bootJar` + `java -jar` + `curl`): success (201),
  capacity exhaustion (409), validation failure (400), malformed enum (400) all behave as expected.
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

Bootstrap, rules, architectural boundaries, the domain model, the reservation use case, and concurrency
hardening are all merged. Currently on `rest-api`, implementing the minimal REST adapter. Next stage
after this one is `documentation` (README, limitations/trade-offs, AI usage disclosure, "given more
time") — the last planned stage. **Branch creation is the user's step, not Claude Code's** —
implementation of the next stage begins only after the user creates and checks out that branch and
confirms.

## Session Resumption Notes

- When resuming, read `progress.md` for overall status and this file for recent decisions.
- Always check `git branch --show-current` and `git status` before making changes.
- Do not start the next branch until the user confirms `master` is in the expected state and tells
  Claude Code which branch to work on.
- Build command: `./gradlew build`
- Test command: `./gradlew test`
- No Docker/Testcontainers dependency for tests — plain `./gradlew test` is sufficient.
