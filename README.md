# Scribl

A performance-driven multiplayer drawing-and-guessing game (inspired by
skribbl.io). The project evolves a deliberately simple implementation into a
measured, scalable one, one milestone at a time.

- `backend/` — Java 21 + Spring Boot (modular monolith)
- `frontend/` — React + TypeScript (Vite) — added in a later V1 step
- `load-tests/` — k6 harness — added in V2
- `docs/` — architecture, roadmap, decisions, benchmarks

Current status: **V1 baseline in progress** (REST + PostgreSQL + polling).

## Prerequisites

- JDK 21 (`brew install openjdk@21`) — note Homebrew installs it keg-only.
- Maven 3.9+
- PostgreSQL database (this project is configured against Neon).
- Node.js 20+ (only needed once the frontend is added).

## Configuration

Secrets are not committed. Copy the template and fill in your database details:

```bash
cp backend/.env.example backend/.env
```

`backend/.env` is git-ignored and imported automatically by Spring Boot. Use a
JDBC URL (note the `jdbc:` prefix) and keep `sslmode=require`:

```
DB_URL=jdbc:postgresql://<host>/<database>?sslmode=require
DB_USER=<username>
DB_PASSWORD=<password>
```

## Running the backend

Homebrew's JDK is keg-only, so point Maven at it explicitly:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn spring-boot:run
```

The app starts on `http://localhost:8081` (port 8080 is left free for a local
Jenkins). Health check: `GET /actuator/health`.
Flyway applies `db/migration` on startup (schema is created automatically).

## Tests

Integration tests use embedded H2 in PostgreSQL mode and never touch the real
database, so no configuration is required:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test
```

## Documentation

- `docs/architecture.md` — how the system works today
- `docs/development-roadmap.md` — milestone plan and status
- `docs/decisions/` — architecture decision records
- `docs/benchmarks/` — measured results (populated from V2 onward)
