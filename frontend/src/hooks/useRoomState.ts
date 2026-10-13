import { useEffect, useState } from 'react'
import { api } from '../api/client'
import type { RoomState } from '../api/types'

interface RoomStateResult {
  state: RoomState | null
  error: string | null
}

/**
 * Polls the room state on an interval. V1 uses HTTP polling by design; later
 * milestones replace this with push. Polling starts once a token is available
 * and stops on unmount. A failed poll surfaces an error but does not stop the
 * loop, so a transient outage recovers on its own.
 */
export function useRoomState(code: string, token: string | null, intervalMs = 1500): RoomStateResult {
  const [state, setState] = useState<RoomState | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!token) {
      return
    }
    let active = true

    const tick = async () => {
      try {
        const next = await api.getState(code, token)
        if (active) {
          setState(next)
          setError(null)
        }
      } catch (e) {
        if (active) {
          setError(e instanceof Error ? e.message : 'connection lost')
        }
      }
    }

    void tick()
    const timer = window.setInterval(tick, intervalMs)
    return () => {
      active = false
      window.clearInterval(timer)
    }
  }, [code, token, intervalMs])

  return { state, error }
}
