# Active Context — Car Rental

## Current State

- Branch: `documentation`
- Based on: `master` after `black-box-testing` was merged (PR #7)
- Task: finalizing documentation — README, MkDocs internal docs site (Material theme, Mermaid
  diagrams), limitations/trade-offs, AI usage disclosure, "future improvements". This is the last planned
  stage.

## Most Recent Decisions

- MkDocs (Material theme) + Mermaid diagrams for internal docs, superseding the earlier "no MkDocs"
  call — the user explicitly asked for it this stage. `.clinerules` Technology stack updated
  accordingly.
- Did a documentation consolidation pass first: several decisions (Hexagonal architecture, per-car-type
  locking, black-box testing, REST request shape) had their rationale repeated near-verbatim across
  `.clinerules`, `techContext.md`, and this file. Trimmed each to state the conclusion once with a
  pointer to its ADR (or to `.clinerules`' relevant rules section where there's no ADR), rather than
  re-deriving the reasoning in every location.

## Still-open ideas for "future improvements" (raised and deliberately deferred, not forgotten)

- Admin endpoints to inspect/adjust fleet state at runtime — deferred as new scope: `CarTypeInventory`
  is currently immutable, so this needs a new domain rule (what happens when shrinking a fleet below
  its active-reservation count) beyond what's needed to solve test isolation.
- A `Clock`/`FakeClock` bean — skipped: nothing in the domain depends on "now" today, so it wouldn't
  change any production behavior; would become genuinely useful if a time-dependent rule (e.g. "can't
  reserve in the past") is ever added.

## Branching Workflow

1. User creates branch, tells Claude Code.
2. Claude Code implements with individual commits.
3. User pushes, reviews, may request fixes.
4. User merges to `master`, pulls, tells Claude Code.
5. Claude Code acknowledges, suggests next branch name.
6. Repeat.

## Project Status

Every stage through `black-box-testing` is merged to `master`. Currently on `documentation`, the last
planned stage — once this is reviewed and merged, the project is complete.

## Session Resumption Notes

- When resuming, read `progress.md` for overall status and this file for recent decisions.
- Always check `git branch --show-current` and `git status` before making changes.
- Build command: `./gradlew build`. Test command: `./gradlew test`.
- Docs site: `pip install -r requirements.txt && mkdocs serve` (see `mkdocs.yml`).
- No Docker/Testcontainers dependency for tests — plain `./gradlew test` is sufficient.
