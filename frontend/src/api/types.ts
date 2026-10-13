/** DTOs mirroring the backend JSON. Field names match the API exactly. */

export type RoomStatus = 'LOBBY' | 'IN_PROGRESS' | 'FINISHED'

export type DrawingAction = 'POINT' | 'CLEAR'

export interface RoomSession {
  roomCode: string
  playerId: number
  playerToken: string
}

export interface PlayerDto {
  id: number
  name: string
  score: number
  isHost: boolean
  connected: boolean
}

export interface RoomState {
  code: string
  status: RoomStatus
  totalRounds: number
  currentRoundNumber: number
  hostPlayerId: number | null
  currentDrawerId: number | null
  roundEndsAt: string | null
  secondsRemaining: number | null
  currentWord: string | null
  players: PlayerDto[]
}

export interface Leaderboard {
  code: string
  status: RoomStatus
  standings: PlayerDto[]
}

export interface GuessResult {
  correct: boolean
  awardedPoints: number
}

export interface DrawingPoint {
  seq: number
  action: DrawingAction
  x: number | null
  y: number | null
  color: string | null
  size: number | null
  tool: string | null
  playerId: number
}

export interface RoundDrawing {
  roundNumber: number
  sinceSeq: number
  points: DrawingPoint[]
}

export interface DrawingAppended {
  seq: number
}

export interface DrawingRequest {
  action: DrawingAction
  x?: number
  y?: number
  color?: string
  size?: number
  tool?: string
}

export interface ApiErrorBody {
  timestamp: string
  status: number
  error: string
  message: string
}
