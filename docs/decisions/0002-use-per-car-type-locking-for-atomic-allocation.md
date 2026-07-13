# 0002. Use per-car-type locking for atomic reservation allocation

## Status

Accepted

## Context

`CarInventoryRepository.reserve(carType, period)` must atomically check availability
(`CarTypeInventory.hasCapacityFor`) and record a new reservation, so that concurrent calls for the same
car type and an overlapping period cannot both succeed when only one matching unit remains — this is one
of this project's explicitly named non-trivial areas. Reservations for different car types are entirely
independent — the domain rule itself (`CarTypeInventory.hasCapacityFor`) already operates per `CarType`,
so there is no correctness reason to serialize a SEDAN request behind a VAN request.

## Decision Drivers

- Must never allow overbooking under concurrent access, under any interleaving.
- Correctness matters far more than throughput for a simulated, single-process system — but
  introducing avoidable contention between unrelated car types would be a real design smell, not a
  neutral simplification.
- Must stay simple enough to implement, test, and explain within this project's small, focused scope.
- Must keep the domain's pure `CarTypeInventory.hasCapacityFor` rule completely unaware of how atomicity
  is achieved (the separation ADR 0001 is built around).

## Considered Options

1. A single global lock guarding the whole repository (`synchronized` on `this`, or one shared
   `ReentrantLock` field).
2. A dedicated lock object per `CarType`, held for the whole check-then-record step (selected).
3. Lock-free optimistic concurrency — e.g. an `AtomicReference`-based compare-and-swap retry loop, or
   building on `ConcurrentHashMap`'s atomic `compute` family, per car type.

## Advantages and Disadvantages

### Option 1 — single global lock

- Simplest possible correct implementation: one lock, no per-type bookkeeping.
- Serializes reservation attempts for *unrelated* car types (a SEDAN request blocks behind a VAN
  request) even though the domain rule is already scoped per type — unnecessary contention with no
  correctness benefit.

### Option 2 — per-car-type lock (selected)

- Correct: fully serializes conflicting attempts for the *same* type, never blocks attempts for a
  *different* type.
- Matches the domain's own partitioning — the locking granularity mirrors the business boundary
  (`CarTypeInventory` is already a per-type concept).
- Simple to implement, test, and explain: one dedicated lock object per configured car type,
  `synchronized` around the existing check-then-record logic — no new concurrency primitives to reason
  about beyond intrinsic locks.
- Slightly more bookkeeping than a single lock: one lock object and one reservation list per car type,
  both fixed once at construction time.

### Option 3 — lock-free optimistic concurrency

- Avoids blocking entirely; could offer better throughput under heavy contention.
- Meaningfully more complex to implement correctly (retry loops, reasoning about visibility and retry
  storms for a list-shaped piece of state) and harder to reason about and explain than a lock.
- Disproportionate engineering effort for a small project whose core requirement is correctness
  under concurrency, not throughput under load.

## Decision

Use a dedicated lock object per configured `CarType`, created once at construction and never replaced.
Each type's accepted reservations are also stored separately (an `EnumMap<CarType, List<Reservation>>`),
so concurrent operations on different types never touch the same mutable list. `reserve(carType, period)`
synchronizes on that type's lock for the entire check-then-record step.

Lock objects are private, plain `new Object()` instances — not the `CarType` enum constants themselves —
to avoid the classic hazard of synchronizing on an object other, unrelated code could also lock on.

## Rationale

This mirrors, at the concurrency layer, the partitioning the domain rule already has, giving the
strongest correctness guarantee (no overbooking, ever) without introducing avoidable contention between
unrelated car types. `synchronized` blocks are simple, well-understood, and easy to reason about and
explain — appropriate for a project whose scope is kept intentionally small. A lock-free approach would
be more sophisticated but disproportionate: this project's core requirement is correctness under
concurrency, not throughput under load.

## Consequences

- `InMemoryCarInventoryRepository` holds three `EnumMap<CarType, ...>` fields populated once at
  construction (inventories, per-type reservation lists, per-type locks). The key set never changes
  after construction, so map-level lookups need no synchronization of their own — only the per-type
  reservation lists' *contents* are mutated, under that type's lock.
- Concurrency tests race many threads for the same (limited) car type and assert exactly `totalUnits`
  successes and the rest rejected, proving the property directly rather than only by code inspection —
  see `InMemoryCarInventoryRepositoryConcurrencyTest`.
- Two *different* car types can proceed fully in parallel; this is a design property, not separately
  proven by a test (asserting true parallelism deterministically in a unit test is impractical), and is
  documented here as a reasoned claim rather than a tested one.
- `ReservationService` is consequently a one-line pass-through, not an orchestrator: the domain rule
  (`CarTypeInventory#hasCapacityFor`) is invoked from inside `InMemoryCarInventoryRepository` (the lock
  holder), not from the application layer — inverting the usual hexagonal expectation that the
  application orchestrates domain logic while the adapter stays a dumb I/O boundary. Splitting
  `CarInventoryRepository#reserve` into separate find/save calls so the service could invoke the rule
  itself would reopen exactly the race this locking exists to prevent: two concurrent calls could both
  read capacity as available and both write an overlapping reservation. A transactional persistence
  store (a real database, using `@Transactional` with row-level locking, a unique constraint, or
  optimistic-locking retry) would let the domain check move back into the service, with the database's
  own isolation providing the atomicity guarantee instead of an application-visible Java lock.

## Known Limitations / Risks

- `synchronized` blocks the calling thread while another reservation attempt for the same type is in
  progress; under very heavy contention for a single popular car type this would limit throughput. Not a
  concern at this project's scope.
- If the system needed to scale beyond a single JVM (e.g. multiple instances behind a load balancer),
  in-memory per-type locks would no longer provide a global guarantee — a real deployment would need a
  shared store with proper transactional or optimistic-locking semantics (e.g. a database with a unique
  constraint or row-level locking). Out of scope for this simulated, single-process project.
- Lock objects are created once per configured `CarType` at construction; a repository that needed to
  support car types added dynamically after construction would need additional care (e.g.
  `computeIfAbsent`) — not needed here since the fleet's car types are fixed at construction.
