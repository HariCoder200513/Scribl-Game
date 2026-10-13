# Scribl frontend

React + TypeScript (Vite) client for the Scribl game.

## Running

Start the backend first (see the repository root `README.md`), then:

```bash
npm install
npm run dev
```

Open http://localhost:5173.

During development the client calls the API on the same origin under `/api`,
and the Vite dev server proxies those requests to the backend at
`http://localhost:8081` (see `vite.config.ts`). That avoids CORS entirely.

Override the proxy target when the backend is elsewhere:

```bash
VITE_API_PROXY_TARGET=http://localhost:8081 npm run dev
```

To point the built client at a different API base directly (no proxy), set
`VITE_API_BASE_URL` (see `.env.example`).

## Scripts

- `npm run dev` — dev server with hot reload
- `npm run build` — type-check (`tsc -b`) and production build
- `npm run preview` — serve the production build locally
- `npm run lint` — Oxlint

## Structure

```
src/
  api/         typed API client and DTOs
  components/  shared UI (player list, etc.)
  hooks/       useRoomState (polling)
  lib/         session persistence
  pages/       Home, Lobby, Game
```
