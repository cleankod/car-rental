# Active Context — Car Rental

## Current State

- Branch: `architecture-boundaries`
- Based on: `master` after `project-scaffolding` was merged (PR #1)
- Task: establishing the package skeleton and ADR 0001 for the architecture stage. No domain/feature
  behaviour implemented yet — that starts in `domain-model`.

## Most Recent Decisions

- Selected lightweight Hexagonal Architecture over plain layered — evaluated per `.clinerules`
  "Architecture rules", not defaulted; full rationale now recorded in
  `docs/decisions/0001-use-lightweight-hexagonal-architecture.md`.
- Package skeleton created: `domain`, `application` (+ `port.in`, `port.out`), `adapter.out.persistence`
  — each a `package-info.java` only, no classes yet. `config` and `adapter.in.rest` deliberately not
  created yet — nothing needs Spring wiring or a REST adapter until later stages.
- ADR numbering starts at `0001` for this decision — Java 25 and Gradle Groovy DSL were fixed inputs to
  the assignment, not evaluated choices, so they don't get ADRs of their own.
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

Bootstrap and rules are merged. Currently on `architecture-boundaries`, adding the package skeleton and
ADR 0001. Next stage after this one is `domain-model`. **Branch creation is the user's step, not
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
