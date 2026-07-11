# CLAUDE.md

## Source of truth

This project's rules — architecture, tech stack, domain/exception conventions, testing conventions,
commit conventions, and branching workflow — are defined in `.clinerules` at the repo root. Read it in
full before doing any work here. It applies to Claude Code exactly as it applies to Cline; wherever it
says "Cline," read that as "the AI assistant," regardless of which tool is active.

## Memory bank

Project state lives in `.cline/`:

- `.cline/projectbrief.md` — what the assignment is and its hard constraints
- `.cline/techContext.md` — tech stack, architecture, domain model, ports, running decision log
- `.cline/activeContext.md` — current branch/task and recent decisions — read this first when resuming
- `.cline/progress.md` — one row per branch/stage, what's merged vs. planned

Read `.cline/activeContext.md`, then `.cline/progress.md`, before starting work in a new session. Update
`activeContext.md` and `progress.md` after completing each branch/stage, per the "Memory bank" section of
`.clinerules`, and include those updates in the branch's own commits — not a separate branch.

## Working across tools

This repo may be worked on by both Cline and Claude Code, possibly in different sessions. Both must:

- Follow `.clinerules` as written — there is no separate/parallel rules file for Claude Code, and this
  file must not accumulate rule content of its own over time.
- Keep `.cline/` current so a session started in either tool can resume cleanly from what the other left.
- Never fork the rules: if a rule needs to change, edit `.clinerules` itself so both tools see the update
  immediately.

## Quick orientation checklist

1. Read `.clinerules` in full.
2. Read `.cline/activeContext.md`, then `.cline/progress.md`.
3. Run `git branch --show-current` and `git status` before making any changes.
4. Confirm the current branch matches what `activeContext.md` says. If it doesn't, stop and ask the user
   before proceeding — it usually means work happened in the other tool since the memory bank was updated.
