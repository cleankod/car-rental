# Testing Strategy

Unit-test only pure, dependency-free classes directly. Everything with an injected collaborator is
proven through the real REST entry point instead of in its own isolated test — so internal refactoring
of a service or adapter can't break tests as long as the observable HTTP behaviour is unchanged. Full
rationale, including the alternatives considered: [ADR 0003](decisions/0003-favor-black-box-tests-through-the-rest-entry-point.md).

## What's tested where

| Test class | Targets | Style |
|---|---|---|
| `RentalPeriodTest`, `ReservationTest`, `CarTypeInventoryTest` | Pure domain records/rules — no collaborators | Unit |
| `ReservationControllerTest` | The whole stack (controller → use case → repository), real Spring context | Black-box, `TestRestTemplate` |
| `InMemoryCarInventoryRepositoryConcurrencyTest` | Atomic allocation under contention | White-box (deliberate exception) |

`ReservationService` and `InMemoryCarInventoryRepository` have no dedicated test of their own — both
take an injected collaborator, so per the rule above they're proven exclusively through
`ReservationControllerTest`.

## Why concurrency is the one white-box exception

Racing real HTTP requests through `TestRestTemplate` would be slow and non-deterministic, and couldn't
reproduce the tight `CountDownLatch`-synchronized contention needed to reliably hit the last-unit race.
`InMemoryCarInventoryRepositoryConcurrencyTest` calls `repository.reserve(...)` directly from many
threads and asserts the exact success/rejection counts.

## Required scenarios

- A reservation succeeds when a unit of the requested type is available for the whole period.
- A reservation is rejected when no unit of that type remains available.
- Overlapping periods for the same type are correctly detected and rejected.
- Touching (non-overlapping) periods are correctly treated as *not* overlapping — tested deliberately,
  not left accidental.
- Capacity is tracked independently per car type.
- Existing reservations that individually overlap a candidate, but don't overlap each other, correctly
  leave enough capacity for the candidate (one vehicle can serve the non-overlapping existing ones
  sequentially, freeing another for the candidate) — a fragmented-inventory case that a naive
  overlap-count would incorrectly reject.
- Concurrent reservation attempts for an overlapping period, with only one unit remaining, result in
  exactly one success and the rest rejected.
