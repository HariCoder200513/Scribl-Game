# ADR-0001: Modular monolith with PostgreSQL as the single source of truth

- **Status:** accepted
- **Date:** 2026-10-11
- **Milestone:** V1

## Context

We are building a small multiplayer game and want a starting point that is easy
to understand, easy to test, and easy to measure. The project's explicit goal is
to earn performance through evidence, not to start optimised.

## Decision

Build V1 as a single Spring Boot modular monolith using REST and PostgreSQL.
Game rules, state, and authorization live on the server. No Redis, WebSockets,
message broker, or microservices. Drawing points are persisted one row per
point and read back with polling.

## Rationale

- A single deployable and a single database is the smallest thing that works.
- Keeping all logic server-side establishes the security invariants early
  (never trust the client) so later optimisations cannot quietly break them.
- The intentionally naive data path is the experiment: it gives V2 an honest
  baseline to measure.

## Consequences

- Positive: trivial local setup, clear failure modes, one place to read state.
- Negative: per-point writes and polling create an obvious database/network
  bottleneck; single-instance round scheduling is not multi-instance safe.
- These negatives are, by design, the subjects of V2–V6.
