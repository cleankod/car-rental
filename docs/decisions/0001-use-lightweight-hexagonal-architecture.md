# 0001. Use lightweight Hexagonal Architecture

## Status

Accepted

## Context

The assignment (a simulated Car Rental system: reserve a car of a given type — Sedan, SUV, or Van — for
a date/time and a number of days, with limited inventory per type, proven by unit tests) has exactly one
real integration: an in-memory inventory/reservation store. There is no messaging, no external system,
and no secondary integration to mock. An optional minimal REST adapter may be added later, but the brief
itself only requires unit tests.

`.clinerules`'s own architecture criterion is explicit: Hexagonal/Ports-and-Adapters "earns its cost when
there are multiple real inbound/outbound integrations and at least one is mocked or plausibly swappable,"
and plain layered "is more honest — and faster to build and explain — for a single-integration ...
assignment." Taken literally, this assignment's integration count (one) points at plain layered, not
Hexagonal. This ADR records why lightweight Hexagonal was chosen anyway, rather than defaulting to it or
silently ignoring the criterion.

## Decision Drivers

- The brief explicitly names two areas to demonstrate depth on: clear separation between domain
  behaviour and infrastructure, and atomic allocation under concurrent reservation attempts (i.e. the
  concurrency-control mechanism).
- The two-hour time-box: any abstraction has to earn its keep immediately, not "someday."
- The design must be easy to explain and defend in the interview.
- Honesty about trade-offs: the brief explicitly asks for known limitations/trade-offs to be documented,
  which extends to admitting when a chosen pattern doesn't strictly satisfy its own textbook trigger.

## Considered Options

1. Plain layered (`controller` / `service` / `repository`)
2. Full Hexagonal / Ports and Adapters (multi-adapter form, with `adapter.in`/`adapter.out` subtrees for
   every technology, `UseCase`-suffixed ports for every operation)
3. Lightweight Hexagonal — one inbound port, one outbound port, no adapter subtree for technologies that
   don't exist here (selected)

## Advantages and Disadvantages

### Option 1 — Plain layered

- (+) Fastest to build and explain.
- (+) Matches this assignment's actual integration count (one), per `.clinerules`'s own criterion.
- (-) To isolate the concurrency-control mechanism from the business rule at all, the repository still
  needs to sit behind an interface — `.clinerules` allows this ("still isolate the one or two things
  actually likely to be mocked or swapped ... behind an interface"). Once that interface exists, the gap
  to option 3 is mostly naming, not structure.
- (-) No standard vocabulary for describing the domain/infrastructure boundary in an interview beyond
  "it's behind an interface."

### Option 2 — Full Hexagonal

- (+) Maximum flexibility if the system later grows more integrations.
- (-) Ceremony for technologies that don't exist here — `adapter.in`/`adapter.out` subtrees for messaging
  and secondary integrations that this assignment never has.
- (-) Directly contradicts `.clinerules`'s own stated trigger for this pattern (multiple real
  integrations, one mocked/swappable) — not the case here.
- (-) Disproportionate for a ~2-hour take-home; risks reading as over-engineering in the interview.

### Option 3 — Lightweight Hexagonal (selected)

- (+) One named port (`CarInventoryRepository`) plus one in-memory adapter costs almost nothing more
  than option 1's plain interface + implementation.
- (+) Cleanly isolates the concurrency-control mechanism (how atomicity is achieved) from the domain rule
  it protects (no overbooking) — one of the two areas the brief names as worth demonstrating depth on.
- (+) Gives a standard vocabulary (port/adapter) for explaining the domain/infrastructure boundary — the
  other named area.
- (+) Package-by-feature collapses to a root-level split, since the assignment has exactly one cohesive
  feature (car reservation) — no redundant nested feature package.
- (-) Slightly more package/interface ceremony than option 1 for what is, strictly, a single-integration
  system — this is the trade-off being consciously made, not overlooked.
- (-) Departs from the letter of `.clinerules`'s own Hexagonal-trigger criterion; disclosed here rather
  than silently ignored.

## Decision

Adopt lightweight Hexagonal Architecture, scoped down from the full multi-adapter form: a single
inbound port (`ReserveCarUseCase`), a single outbound port (`CarInventoryRepository`), package-by-feature
collapsed to a root-level split (`domain` / `application` / `adapter` / `config`), and no adapter subtree
for technologies that don't exist in this assignment (messaging, secondary integrations).

## Rationale

The classic Hexagonal trigger (multiple real integrations, one mocked/swappable) does not strictly apply
— there is exactly one real boundary. What justifies the pattern anyway is that its incremental cost over
plain layered is close to zero at this scale (one port, one adapter), while it buys a materially cleaner
way to demonstrate the two things the brief explicitly calls out as worth investing depth in: domain/
infrastructure separation, and isolating the concurrency-control mechanism from the invariant it protects.
Paying a near-zero cost for a clearer interview narrative on the graded non-trivial areas is judged worth
it; the alternative (plain layered) would have been an equally defensible, and slightly more "honest," choice by the letter of `.clinerules`'s own criterion.

## Consequences

- Package layout: `domain` (pure, framework-free) / `application` (+ `port.in`, `port.out`) / `adapter`
  (`out.persistence` now; `in.rest` only if the optional REST stage is undertaken) / `config` (only once
  something actually needs Spring wiring).
- `ReserveCarUseCase` is the one inbound port; `CarInventoryRepository` is the one outbound port. No
  further `UseCase`/port ceremony is added unless a second real operation or integration appears.
- The persistence adapter's concurrency-control mechanism (e.g. per-type locking, compare-and-swap) can
  change without touching `domain` or `application` — this boundary is the concrete, testable payoff of
  the pattern for this assignment.

## Known Limitations / Risks

- If asked in the interview "why Hexagonal for a system with one integration," the honest answer is: it
  doesn't strictly need to be — it was chosen for the domain/infrastructure separation and concurrency-
  isolation narrative at near-zero structural cost, not because the integration count demanded it.
- The classic benefit of Hexagonal — swapping an adapter without touching the application — is never
  actually exercised in this assignment, since there is only ever one real implementation of
  `CarInventoryRepository`. Its value here is entirely the separation/narrative benefit, not realized
  future swappability.
