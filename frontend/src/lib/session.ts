import type { RoomSession } from '../api/types'

export interface Session extends RoomSession {
  playerName: string
}

const STORAGE_KEY = 'scribl.session'

/** The player's current room session, persisted so a refresh keeps them in the room. */
export function loadSession(): Session | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as Session
  } catch {
    return null
  }
}

export function saveSession(session: Session): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function clearSession(): void {
  localStorage.removeItem(STORAGE_KEY)
}
