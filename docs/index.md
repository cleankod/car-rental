# Car Rental — Overview

A simulated car rental reservation system: given a car type, a desired start date/time, and a number of
days, reserve one unit of that type if the fleet has capacity — atomically, even under concurrent
requests. Built as a take-home technical assessment.

## What the system does

1. `POST /api/v1/reservations` accepts a car type, start date/time, and number of days.
2. The system checks whether a unit of that type is free for the whole period.
3. If so, the reservation is recorded and returned (`201 Created`); if not, rejected (`409 Conflict`).
4. Capacity is limited and tracked per car type (Sedan / SUV / Van); allocation is atomic even when
   multiple requests race for the last available unit.

## Technology stack

| Layer | Technology |
|---|---|
| Runtime | Java 25 |
| Framework | Spring Boot 4 |
| Architecture | Lightweight Hexagonal (domain / application / adapter / config) |
| Persistence | In-memory, thread-safe (no database — simulated system) |
| Build | Gradle (Groovy DSL) with version catalog |
| Tests | JUnit 5 + AssertJ |
| Docs | MkDocs (this site) |

## Navigation

- **[Architecture](architecture.md)** — package layout, request flow, domain model.
- **[Testing strategy](testing.md)** — what's unit-tested vs. proven through the REST API, and why.
- **[Decisions](decisions/0001-use-lightweight-hexagonal-architecture.md)** — ADRs for the non-trivial
  design choices, with considered alternatives and trade-offs.

See the project README for setup, running tests, known limitations/trade-offs, and the AI-usage
disclosure.
