import { useParams } from 'react-router-dom'
import PlayerList from '../components/PlayerList'
import { useRoomState } from '../hooks/useRoomState'
import { loadSession } from '../lib/session'

/**
 * Placeholder game screen. V1 step 6 only scaffolds routing and polling; the
 * canvas, guess feed and scoring UI arrive in step 7. It shows enough state to
 * confirm the lobby hand-off works end to end.
 */
export default function GamePage() {
  const { code = '' } = useParams()
  const session = loadSession()
  const token = session?.playerToken ?? null
  const { state, error } = useRoomState(code, token)

  if (!session) {
    return (
      <main className="page">
        <div className="card">
          <p className="error">No session for this room. Return home to join.</p>
        </div>
      </main>
    )
  }

  const isDrawer = state?.currentDrawerId === session.playerId

  return (
    <main className="page">
      <div className="card">
        <header className="card-header">
          <h1 className="brand small">Scribl — room {code}</h1>
          <span className={`status status-${state?.status ?? 'LOBBY'}`}>{state?.status ?? '…'}</span>
        </header>

        {error && <p className="error">{error}</p>}

        <div className="hud">
          <div>
            <span className="hud-label">Round</span>
            <span className="hud-value">
              {state?.currentRoundNumber ?? 0} / {state?.totalRounds ?? '?'}
            </span>
          </div>
          <div>
            <span className="hud-label">Time</span>
            <span className="hud-value">{state?.secondsRemaining ?? '–'}s</span>
          </div>
          <div>
            <span className="hud-label">Word</span>
            <span className="hud-value">{isDrawer ? state?.currentWord : '?????'}</span>
          </div>
        </div>

        <section className="panel">
          <h2>Players</h2>
          {state && (
            <PlayerList
              players={state.players}
              currentPlayerId={session.playerId}
              drawerId={state.currentDrawerId}
              showScores
            />
          )}
        </section>

        <p className="muted">Canvas, guesses and scoring arrive in the next step.</p>
      </div>
    </main>
  )
}
