import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'
import PlayerList from '../components/PlayerList'
import { useRoomState } from '../hooks/useRoomState'
import { loadSession, saveSession, type Session } from '../lib/session'

export default function LobbyPage() {
  const { code = '' } = useParams()
  const navigate = useNavigate()
  const [session, setSession] = useState<Session | null>(() => {
    const stored = loadSession()
    return stored && stored.roomCode === code ? stored : null
  })

  const token = session?.playerToken ?? null
  const { state, error } = useRoomState(code, token)

  // Once the host starts, everyone moves to the game screen.
  useEffect(() => {
    if (state?.status === 'IN_PROGRESS') {
      navigate(`/room/${code}/game`)
    }
  }, [state?.status, code, navigate])

  if (!session) {
    return <JoinPrompt code={code} onJoined={setSession} />
  }

  const isHost = state?.hostPlayerId === session.playerId
  const enoughPlayers = (state?.players.length ?? 0) >= 2

  async function startGame() {
    if (!session) {
      return
    }
    try {
      await api.startGame(code, session.playerToken)
    } catch (e) {
      window.alert(e instanceof Error ? e.message : 'could not start the game')
    }
  }

  return (
    <main className="page">
      <div className="card">
        <header className="card-header">
          <h1 className="brand small">Scribl lobby</h1>
          <CopyableCode code={code} />
        </header>

        {error && <p className="error">{error}</p>}

        <section className="panel">
          <h2>Players ({state?.players.length ?? 0}/8)</h2>
          {state ? (
            <PlayerList players={state.players} currentPlayerId={session.playerId} />
          ) : (
            <p className="muted">Connecting…</p>
          )}
        </section>

        {isHost ? (
          <div className="start-block">
            <button className="primary" disabled={!enoughPlayers} onClick={startGame}>
              Start game
            </button>
            {!enoughPlayers && <p className="muted">Waiting for at least one more player…</p>}
          </div>
        ) : (
          <p className="muted">Waiting for the host to start…</p>
        )}
      </div>
    </main>
  )
}

function CopyableCode({ code }: { code: string }) {
  const [copied, setCopied] = useState(false)

  async function copy() {
    try {
      await navigator.clipboard.writeText(code)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1500)
    } catch {
      // clipboard may be unavailable (insecure context); the code is visible anyway
    }
  }

  return (
    <button className="room-code" onClick={copy} title="Copy room code">
      <span className="room-code-label">Room code</span>
      <span className="room-code-value">{code}</span>
      <span className="room-code-hint">{copied ? 'copied!' : 'tap to copy'}</span>
    </button>
  )
}

function JoinPrompt({ code, onJoined }: { code: string; onJoined: (session: Session) => void }) {
  const nameRef = useRef<HTMLInputElement>(null)
  const [name, setName] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const trimmedName = name.trim()

  async function join() {
    setError(null)
    if (trimmedName === '') {
      setError('Please enter your name first.')
      nameRef.current?.focus()
      return
    }
    setBusy(true)
    try {
      const session = await api.joinRoom(code, trimmedName)
      const full: Session = { ...session, playerName: trimmedName }
      saveSession(full)
      onJoined(full)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'could not join room')
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="page">
      <div className="card">
        <h1 className="brand small">Join {code}</h1>
        <label className="field">
          <span>
            Your name <span className="required">required</span>
          </span>
          <input
            ref={nameRef}
            value={name}
            maxLength={32}
            autoFocus
            onChange={(e) => setName(e.target.value)}
          />
        </label>
        <button type="button" className="primary" disabled={busy} onClick={join}>
          Join room
        </button>
        {error && <p className="error">{error}</p>}
      </div>
    </main>
  )
}
