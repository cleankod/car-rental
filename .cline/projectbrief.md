# Project Brief — Car Rental

## What This Is

A simulated Car Rental system, designed and implemented using object-oriented principles. A
production-ready solution is not the goal; effort is scoped to roughly two hours and focused on depth on
a few non-trivial areas rather than breadth across many.

## Required Flow

- The system allows reservation of a car of a given type at a desired date and time for a given number
  of days.
- There are 3 car types: Sedan, SUV, and Van.
- The number of cars of each type is limited.
- Unit tests must prove the system satisfies these requirements.

There is no prescribed API contract (no endpoint paths, field names, or message formats given) — only
the reservation behaviour and its proof via unit tests are required. Any HTTP surface is an optional
addition, not a core requirement.

## Key Constraints

- Expected scope: ~2 hours of work. The implementation should stay proportional to that — a
  production-shaped slice, not a full product.
- Favour depth in a few areas of genuine interest over breadth across many features.
- The system is explicitly simulated: persistence and infrastructure may be simulated/in-memory. This
  does not extend to skipping the reservation behaviour, the limited-inventory constraint, overlap
  handling, or unit tests. Those are the core requirements.
- Known limitations and trade-offs must be clearly and explicitly documented.
- AI-based assistance is permitted, but all design/implementation decisions should remain understood and
  explainable, with emphasis on the non-trivial areas chosen for depth.

## Repository

- Local project, bootstrapped from a personal starter template.
- Branch strategy: one feature branch per stage, reviewed and merged to `master` by the user (see
  "Branching workflow" in `.clinerules`).
