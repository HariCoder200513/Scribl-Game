import type {
  DrawingAppended,
  DrawingRequest,
  GuessResult,
  Leaderboard,
  RoomSession,
  RoomState,
  RoundDrawing,
} from './types'

const BASE = import.meta.env.VITE_API_BASE_URL ?? ''
const PLAYER_TOKEN_HEADER = 'X-Scribl-Player-Token'

/** Error thrown for any non-2xx API response, carrying the HTTP status. */
export class ApiErrorException extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiErrorException'
    this.status = status
  }
}

interface RequestOptions {
  method?: string
  token?: string
  body?: unknown
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', token, body } = options
  const headers: Record<string, string> = {}
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (token) {
    headers[PLAYER_TOKEN_HEADER] = token
  }

  const response = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (!response.ok) {
    let message = `Request failed (${response.status})`
    try {
      const parsed = (await response.json()) as { message?: string }
      if (parsed.message) {
        message = parsed.message
      }
    } catch {
      // response had no JSON body; keep the default message
    }
    throw new ApiErrorException(response.status, message)
  }

  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}

export const api = {
  createRoom(playerName: string, totalRounds: number): Promise<RoomSession> {
    return request<RoomSession>('/api/rooms', {
      method: 'POST',
      body: { playerName, totalRounds },
    })
  },

  joinRoom(code: string, playerName: string): Promise<RoomSession> {
    return request<RoomSession>(`/api/rooms/${code}/join`, {
      method: 'POST',
      body: { playerName },
    })
  },

  getState(code: string, token: string): Promise<RoomState> {
    return request<RoomState>(`/api/rooms/${code}/state`, { token })
  },

  startGame(code: string, token: string): Promise<void> {
    return request<void>(`/api/rooms/${code}/start`, { method: 'POST', token })
  },

  getLeaderboard(code: string, token: string): Promise<Leaderboard> {
    return request<Leaderboard>(`/api/rooms/${code}/leaderboard`, { token })
  },

  submitGuess(code: string, token: string, text: string): Promise<GuessResult> {
    return request<GuessResult>(`/api/rooms/${code}/guesses`, {
      method: 'POST',
      token,
      body: { text },
    })
  },

  appendDrawing(code: string, token: string, drawing: DrawingRequest): Promise<DrawingAppended> {
    return request<DrawingAppended>(`/api/rooms/${code}/drawing`, {
      method: 'POST',
      token,
      body: drawing,
    })
  },

  fetchDrawing(code: string, token: string, sinceSeq: number, roundNumber?: number): Promise<RoundDrawing> {
    const params = new URLSearchParams({ sinceSeq: String(sinceSeq) })
    if (roundNumber !== undefined) {
      params.set('roundNumber', String(roundNumber))
    }
    return request<RoundDrawing>(`/api/rooms/${code}/drawing?${params.toString()}`, { token })
  },
}
