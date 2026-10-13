import type { PlayerDto } from '../api/types'

interface PlayerListProps {
  players: PlayerDto[]
  currentPlayerId: number
  drawerId?: number | null
  showScores?: boolean
}

export default function PlayerList({ players, currentPlayerId, drawerId, showScores }: PlayerListProps) {
  return (
    <ul className="players">
      {players.map((player) => {
        const classes = ['player']
        if (player.id === currentPlayerId) {
          classes.push('is-you')
        }
        if (player.id === drawerId) {
          classes.push('is-drawing')
        }
        return (
          <li key={player.id} className={classes.join(' ')}>
            <span className="player-name">
              {player.name}
              {player.isHost && <span className="badge">host</span>}
              {player.id === currentPlayerId && <span className="badge you">you</span>}
              {player.id === drawerId && <span className="badge drawing">drawing</span>}
            </span>
            {showScores && <span className="player-score">{player.score}</span>}
          </li>
        )
      })}
    </ul>
  )
}
