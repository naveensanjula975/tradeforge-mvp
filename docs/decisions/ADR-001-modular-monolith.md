# ADR-001: Modular Monolith Architecture for TradeForge MVP

**Date:** 2026-07-13  
**Status:** Accepted  
**Author:** TradeForge Team

---

## Context

TradeForge is a mini electronic exchange with a matching engine, order management, account management, and real-time WebSocket updates. We need an architecture that allows us to:

- Ship a working MVP quickly (10–12 weeks).
- Maintain a correct, testable matching engine with deterministic behaviour.
- Keep the team small and avoid distributed-systems complexity during the early phase.
- Provide a clear migration path to microservices when scale requires it.

---

## Decision

We will build the TradeForge MVP as a **modular monolith** — a single deployable Spring Boot application divided into well-defined, loosely-coupled modules:

| Module         | Responsibility                                         |
|----------------|--------------------------------------------------------|
| `auth`         | User registration, login, JWT                          |
| `instrument`   | Instrument domain, market status                       |
| `order`        | Order aggregate, state machine, validation             |
| `matching`     | In-memory order book and matching engine               |
| `account`      | Cash balances, positions, risk checks                  |
| `notification` | WebSocket event publishing                             |

Each module owns its own domain model, service layer, repository, and API slice. Cross-module communication happens through **domain events** or explicit service interfaces — never by reaching into another module's repository directly.

---

## Reasons

1. **Speed to market** — A single deployable eliminates network calls, distributed transactions, and deployment complexity during the MVP phase.
2. **Correctness first** — The matching engine must be tested with deterministic, single-threaded logic. A monolith makes this straightforward.
3. **Operational simplicity** — One application, one database, one Docker Compose file. Easier to run locally and to debug.
4. **Clear module boundaries** — Enforcing module boundaries now means we can extract services later with minimal refactoring.
5. **Team size** — A modular monolith is appropriate for a team of one to five engineers.

---

## Alternatives Considered

### Microservices from Day One

| Pros | Cons |
|------|------|
| Independent scaling and deployment | Distributed transactions are very complex for an order book |
| Technology flexibility per service | Requires service mesh, tracing, and distributed logging immediately |
| — | Network latency between matching engine and order service |
| — | Much slower to develop and test in early stages |

**Rejected** — The operational overhead is not justified at MVP scale.

### Event-Driven with Kafka

| Pros | Cons |
|------|------|
| Decoupled producers and consumers | Adds broker dependency and ordering guarantees complexity |
| Replay-able event log | Increases local development complexity |
| Natural audit trail | Kafka expertise required |

**Delayed** — Kafka will be introduced in a future milestone when message volume justifies it.

### Redis for Order Book State

**Delayed** — The in-memory order book is sufficient for MVP. Redis would be added for clustering/recovery in a later phase.

### FIX Protocol / gRPC API

**Delayed** — REST + WebSocket is sufficient for the MVP dashboard. FIX and gRPC can be added when institutional connectivity is required.

---

## Consequences

### Positive

- Faster development and testing cycles.
- Simpler local development (one `docker-compose up`).
- Easier to ensure matching engine correctness with unit tests.
- All modules in one codebase makes refactoring straightforward.

### Negative

- Cannot independently scale individual components (e.g., the matching engine only).
- A bug in one module can affect the whole application.
- Requires discipline to maintain module boundaries without package-level enforcement.

---

## Migration Path to Microservices

When the following triggers occur, individual modules can be extracted:

1. **Matching engine** — When order throughput requires a dedicated process with isolated threads.
2. **Notification service** — When WebSocket connections exceed what a single node can handle.
3. **Account/risk service** — When regulatory requirements demand audit isolation.

Steps for each extraction:
1. Replace in-process method calls with a domain event (Kafka topic or gRPC call).
2. Move the module's code to a new service repository.
3. Deploy independently with its own database schema.

Because module boundaries are enforced from the start, each extraction is a controlled lift-and-shift rather than a major refactor.

---

## Related

- [README.md](../../README.md)
- Task M0.5 in `TradeForge_Step_by_Step_Task_List.md`
