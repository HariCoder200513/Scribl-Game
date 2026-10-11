# Development roadmap

Milestones are implemented sequentially. Each milestone must keep the
application working and be backed by tests. Performance changes require a
before/after measurement; no benchmark numbers are invented.

| Version | Theme                                   | Status      |
|---------|-----------------------------------------|-------------|
| V1      | Deliberately naive baseline (REST + DB) | in progress |
| V2      | Reproducible benchmarks (k6)            | planned     |
| V3      | WebSockets replace polling              | planned     |
| V4      | Drawing traffic + DB write optimisation | planned     |
| V5      | Redis, state management, recovery       | planned     |
| V6      | Scaling and production readiness        | planned     |

## V1 — baseline (current)

Scope: rooms, join by code, lobby, start game, word selection, canvas with
strokes/colors/eraser, guessing, scoring, round timers, multiple rounds,
leaderboard, basic disconnect/reconnect, server-side authorization. REST +
PostgreSQL + HTTP polling. Per-point persistence.

### Implementation order

1. Repo scaffold, Maven project skeleton, Flyway migrations, `.env` wiring.
2. Room create/join/state + player-token identity + validation. Tests.
3. Lobby start + round lifecycle (word select, timer, sweeper). Tests.
4. Drawing ingest / `?sinceSeq=N` retrieval / clear + drawer authorization. Tests.
5. Guessing + scoring + round end + leaderboard. Tests.
6. Frontend scaffold (Vite + React + TS), API client, Home + Lobby.
7. Game screen: canvas draw, polled render, chat/guesses, timer, scores.
8. Leaderboard + basic reconnect/disconnect handling.
9. End-to-end integration test; build + full test run.
10. Documentation close-out (architecture, ADRs, engineering notes).

## Later milestones (outline only)

- **V2**: k6 load harness; measure throughput, P50/P95/P99, error rate, DB
  query/write counts, CPU/memory across increasing player counts. Store scripts,
  config, raw results, and a Markdown report under `load-tests/` and
  `docs/benchmarks/`.
- **V3**: replace polling with WebSockets; preserve ordering and per-room
  authorization; benchmark old vs new under comparable workloads.
- **V4**: batch points into strokes/events, throttle/flush, dedupe, trim
  payloads, separate durable history from transient drawing state; measure each
  change separately.
- **V5**: introduce Redis when a concrete need is demonstrated; shared room
  state, cross-instance delivery, reconnect snapshots, room expiry, race
  handling at round transitions, idempotent commands.
- **V6**: multiple instances, connection distribution, connection-pool
  behaviour, structured logging/metrics/health/graceful shutdown, failure and
  recovery testing, documented capacity assumptions.
