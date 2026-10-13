import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { saveSession } from '../lib/session'

export default function HomePage() {
  const navigate = useNavigate()
  const nameRef = useRef<HTMLInputElement>(null)
  const codeRef = useRef<HTMLInputElement>(null)
  const [name, setName] = useState('')
  const [joinCode, setJoinCode] = useState('')
  const [totalRounds, setTotalRounds] = useState(3)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const trimmedName = name.trim()

  function requireName(): boolean {
    if (trimmedName === '') {
      setError('Please enter your name first.')
      nameRef.current?.focus()
      return false
    }
    return true
  }

  async function createRoom() {
    setError(null)
    if (!requireName()) {
      return
    }
    setBusy(true)
    try {
      const session = await api.createRoom(trimmedName, totalRounds)
      saveSession({ ...session, playerName: trimmedName })
      navigate(`/room/${session.roomCode}`)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'could not create room')
    } finally {
      setBusy(false)
    }
  }

  async function joinRoom() {
    setError(null)
    if (!requireName()) {
      return
    }
    if (joinCode.trim() === '') {
      setError('Please enter a room code.')
      codeRef.current?.focus()
      return
    }
    setBusy(true)
    try {
      const session = await api.joinRoom(joinCode.trim().toUpperCase(), trimmedName)
      saveSession({ ...session, playerName: trimmedName })
      navigate(`/room/${session.roomCode}`)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'could not join room')
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="page">
      <div className="card">
        <h1 className="brand">Scribl</h1>
        <p className="tagline">Draw. Guess. Score.</p>

        <label className="field">
          <span>
            Your name <span className="required">required</span>
          </span>
          <input
            ref={nameRef}
            value={name}
            maxLength={32}
            placeholder="e.g. alice"
            autoFocus
            onChange={(e) => setName(e.target.value)}
          />
        </label>

        <section className="panel">
          <h2>Create a room</h2>
          <label className="field">
            <span>Rounds</span>
            <input
              type="number"
              min={1}
              max={10}
              value={totalRounds}
              onChange={(e) => setTotalRounds(Number(e.target.value))}
            />
          </label>
          <button type="button" className="primary" disabled={busy} onClick={createRoom}>
            Create room
          </button>
        </section>

        <div className="divider">or</div>

        <section className="panel">
          <h2>Join a room</h2>
          <label className="field">
            <span>Room code</span>
            <input
              ref={codeRef}
              value={joinCode}
              maxLength={8}
              placeholder="ABC123"
              onChange={(e) => setJoinCode(e.target.value.toUpperCase())}
            />
          </label>
          <button type="button" className="secondary" disabled={busy} onClick={joinRoom}>
            Join room
          </button>
        </section>

        {error && <p className="error">{error}</p>}
      </div>
    </main>
  )
}
