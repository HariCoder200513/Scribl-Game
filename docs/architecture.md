# Architecture

> Status: V1 (deliberately naive baseline). This document describes the system as
> it exists today, not as it is planned. Planned work lives in `development-roadmap.md`.

## Context

Scribl is a multiplayer drawing-and-guessing game. The goal of the project is
not only to ship the game but to evolve a simple implementation into a
measurable, performant one. V1 intentionally accepts known inefficiencies so
that later milestones can *measure* and *justify* each optimisation.

## V1 system shape

Modular monolith: a single Spring Boot application backed by one PostgreSQL
database. No Redis, no WebSockets, no message broker, no microservices.

```
React + TS (Vite)  --HTTP REST + polling-->  Spring Boot (modular monolith)  --JDBC-->  PostgreSQL (Neon)
```

## Backend package layout (feature-first)

```
com.scribl
  room/      room aggregate, lifecycle, REST controller
  player/    players, opaque token identity
  game/      round lifecycle, word selection, scheduled sweeper
  drawing/   drawing-point ingest and incremental retrieval
  guess/     guess submission, correctness, scoring
  common/    error handling, configuration, request identity
```

Feature-first packaging (rather than layered `controller/service/repository`)
keeps module boundaries explicit, which makes the later WebSocket and Redis
extractions clean.

## Core design decisions (V1)

1. **PostgreSQL is the single source of truth.** No in-memory game engine.
   Services read and write the database directly. This is the naive baseline;
   it makes the database the measured bottleneck for V2/V4.
2. **Opaque player token identity.** On create/join the server issues a
   `playerToken` (UUID). Clients send it on every request; the server derives
   the acting player from the token and never trusts client-supplied ids,
   scores, words, or permissions.
3. **All game rules server-side.** Scores, correct-guess detection, round
   status, and drawing permissions are computed on the server.
4. **Polling for live updates.** Clients poll room state, drawing deltas, and
   the message feed. Intervals are configurable so V2/V3 can tune and compare.
5. **Append-only drawing points, one row per point.** Deliberate V1 baseline.
   Clients reconstruct the canvas by replaying points and fetching only points
   newer than their high-water mark (`?sinceSeq=N`).
6. **Round transitions via a scheduled sweeper**, single-threaded and guarded
   by a row lock. Explicitly single-instance only; multi-instance correctness is
   a V5/V6 concern.

## Game-state model

The **room row is the aggregate root**: status, current round number, current
drawer, current word, and round end time fully describe where the game stands.
Players own their own score. The canvas is a per-round append-only stream of
points. Round history is represented by `rounds`; correct and incorrect guesses
are recorded in `guesses`.

## Data model

| Table            | Purpose                                                        |
|------------------|----------------------------------------------------------------|
| `rooms`          | Aggregate root: status, timers, current drawer/word, version.  |
| `players`        | Membership, score, host flag, connection state, token.         |
| `rounds`         | One row per round: drawer, word, status, timestamps.           |
| `drawing_points` | Append-only canvas events, keyed by `(round_id, seq)`.         |
| `guesses`        | Guess submissions and whether each was correct.                |

## Known V1 limitations (by design)

- Per-point inserts and full-string/polling reads stress the database.
- Polling wastes bandwidth and adds latency compared with push.
- Single-instance round scheduling is not safe across multiple app instances.
- No reconnect recovery beyond re-joining with the stored token.

Each of these maps to a later milestone and is addressed only once measurements
justify the change.
