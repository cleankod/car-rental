# Project Brief — Car Rental

## What This Is

A take-home technical assessment for Charles River Development (a State Street company): design and
implement a simulated Car Rental system using object-oriented principles. A production-ready solution is
not expected; the brief scopes the effort to roughly two hours and explicitly asks for depth on a few
areas rather than breadth across many.

## Required Flow

- The system allows reservation of a car of a given type at a desired date and time for a given number
  of days.
- There are 3 car types: Sedan, SUV, and Van.
- The number of cars of each type is limited.
- Unit tests must prove the system satisfies these requirements.

There is no prescribed API contract (no endpoint paths, field names, or message formats given) — the
brief only requires the reservation behaviour and its proof via unit tests. Any HTTP surface is our own
optional addition, not a graded requirement.

## Key Constraints

- Expected scope: ~2 hours of work. The implementation should stay proportional to that — a
  production-shaped slice, not a full product.
- Favour depth in the areas of genuine expertise over breadth across many features (brief's own
  guidance).
- The system is explicitly simulated: persistence and infrastructure may be simulated/in-memory. This
  does not extend to skipping the reservation behaviour, the limited-inventory constraint, overlap
  handling, or unit tests — those are the graded core.
- Known limitations and trade-offs must be clearly and explicitly documented, per the brief.
- AI-based assistance is permitted, but all design/implementation decisions are mine to explain in the
  interview — the interview will ask me to walk through the code and reasoning, with emphasis on the
  non-trivial areas I chose to invest in.

## Repository

- Local project, bootstrapped from `~/workspace/playground/recruitment-assessment-skeleton/`.
- Branch strategy: one feature branch per stage, reviewed and merged to `master` by the user (see
  "Branching workflow" in `.clinerules`).
