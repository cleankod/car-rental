# Active Context — Car Rental

## Current State

- Branch: `master`
- Based on: fresh bootstrap from `~/workspace/playground/recruitment-assessment-skeleton/`, no commits yet
- Task: bootstrap is complete. No domain or feature implementation has started. No commits have been
  made to this repository yet.

## Most Recent Decisions

- Selected lightweight Hexagonal Architecture over plain layered — evaluated per `.clinerules`
  "Architecture rules", not defaulted; full rationale in `.clinerules` and (once the architecture stage
  lands) `docs/decisions/0003-use-lightweight-hexagonal-architecture.md`.
- Persistence: in-memory only, thread-safe, no DB/migrations.
- No messaging, no secondary/mocked integrations — single synchronous use case.
- Dropped Testcontainers, Actuator, Micrometer, MkDocs from the skeleton's baseline stack — none apply to
  this assignment's scope.
- REST adapter is optional, deferred to its own later stage; the brief only requires unit tests.

## Branching Workflow

1. User creates branch, tells Claude Code.
2. Claude Code implements with individual commits.
3. User pushes, reviews, may request fixes.
4. User merges to `master`, pulls, tells Claude Code.
5. Claude Code acknowledges, suggests next branch name.
6. Repeat.

## Project Status

Bootstrap is complete: rules, memory bank, and Gradle scaffold are in place and build green. The next
stage is `project-rules`. **Do not create that branch — the user creates it manually.** Implementation
begins only after the user confirms the correct branch is checked out.

## Session Resumption Notes

- When resuming, read `progress.md` for overall status and this file for recent decisions.
- Always check `git branch --show-current` and `git status` before making changes.
- Do not start the next branch until the user confirms `master` is in the expected state and tells
  Claude Code which branch to work on.
- Build command: `./gradlew build`
- Test command: `./gradlew test`
- No Docker/Testcontainers dependency for tests — plain `./gradlew test` is sufficient.
