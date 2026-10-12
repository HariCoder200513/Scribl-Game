package com.scribl.game;

import com.scribl.common.ConflictException;
import com.scribl.common.ForbiddenException;
import com.scribl.common.NotFoundException;
import com.scribl.player.Player;
import com.scribl.player.PlayerRepository;
import com.scribl.room.Room;
import com.scribl.room.RoomRepository;
import com.scribl.room.RoomStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Round lifecycle: starting a game, beginning rounds, and advancing/ending them.
 *
 * {@code start} runs on the request thread. Round advancement is driven by the
 * single-threaded {@link RoundScheduler}: expired rooms are re-read under a
 * pessimistic lock so a transition happens exactly once even if the sweep ever
 * overlaps (defensive only — this instance design is intentionally not safe
 * across multiple app instances; that is a V5/V6 concern).
 */
@Service
public class RoundService {

    private static final Logger log = LoggerFactory.getLogger(RoundService.class);

    private final RoomRepository rooms;
    private final PlayerRepository players;
    private final RoundRepository rounds;
    private final WordProvider wordProvider;
    private final Duration roundDuration;

    public RoundService(RoomRepository rooms,
                        PlayerRepository players,
                        RoundRepository rounds,
                        WordProvider wordProvider,
                        @Value("${scribl.round-duration-seconds:60}") long roundDurationSeconds) {
        this.rooms = rooms;
        this.players = players;
        this.rounds = rounds;
        this.wordProvider = wordProvider;
        this.roundDuration = Duration.ofSeconds(roundDurationSeconds);
    }

    /** Host-only; requires a LOBBY room with at least 2 players. */
    @Transactional
    public void start(String code, Player currentPlayer) {
        Room room = getRoomByCode(code);

        if (!room.getHostPlayerId().equals(currentPlayer.getId())) {
            throw new ForbiddenException("only the host can start the game");
        }
        if (room.getStatus() != RoomStatus.LOBBY) {
            throw new ConflictException("game has already started");
        }
        if (players.countByRoomId(room.getId()) < 2) {
            throw new ConflictException("at least 2 players are required to start");
        }

        beginRound(room, 1);
        rooms.save(room);
        log.info("game started room={} drawer={} rounds={}", room.getCode(),
                room.getCurrentDrawerId(), room.getTotalRounds());
    }

    /**
     * Advance a single room in its own transaction.
     *
     * The room is loaded under a pessimistic row lock AFTER the persistence
     * context is otherwise clean, so the expiry check runs on fresh data. If a
     * concurrent sweeper already transitioned the room, the revalidation below
     * is a no-op and no stale state is flushed. This is the exactly-once
     * transition guard that later becomes the cross-instance (V6) contract.
     */
    @Transactional
    public void advanceIfExpired(Long roomId) {
        Room room = rooms.findByIdForUpdate(roomId)
                .orElseThrow(() -> new NotFoundException("room not found"));

        if (room.getStatus() != RoomStatus.IN_PROGRESS) {
            return;
        }
        if (room.getRoundEndsAt() == null || room.getRoundEndsAt().isAfter(Instant.now())) {
            return; // not expired yet
        }

        endRound(room);
        if (room.getCurrentRoundNumber() >= room.getTotalRounds()) {
            finishRoom(room);
            log.info("game finished room={}", room.getCode());
        } else {
            beginRound(room, room.getCurrentRoundNumber() + 1);
            log.info("round advanced room={} round={}", room.getCode(), room.getCurrentRoundNumber());
        }
        rooms.save(room);
    }

    private void beginRound(Room room, int roundNumber) {
        List<Player> ordered = players.findByRoomIdOrderByJoinedAtAsc(room.getId());
        if (ordered.isEmpty()) {
            finishRoom(room);
            return;
        }

        Player drawer = ordered.get((roundNumber - 1) % ordered.size());
        Set<String> used = new HashSet<>(rounds.findWordsByRoomId(room.getId()));
        String word = wordProvider.pick(used);

        Instant now = Instant.now();
        Round round = new Round(room.getId(), roundNumber, drawer.getId(), word,
                RoundStatus.ACTIVE, now);
        rounds.save(round);

        room.setStatus(RoomStatus.IN_PROGRESS);
        room.setCurrentRoundNumber(roundNumber);
        room.setCurrentDrawerId(drawer.getId());
        room.setCurrentWord(word);
        room.setRoundEndsAt(now.plus(roundDuration));
    }

    private void endRound(Room room) {
        Round round = rounds.findByRoomIdAndRoundNumber(room.getId(), room.getCurrentRoundNumber())
                .orElseThrow(() -> new IllegalStateException("active round row missing"));
        round.setStatus(RoundStatus.ENDED);
        round.setEndedAt(Instant.now());
        rounds.save(round);
    }

    private void finishRoom(Room room) {
        room.setStatus(RoomStatus.FINISHED);
        room.setCurrentDrawerId(null);
        room.setCurrentWord(null);
        room.setRoundEndsAt(null);
    }

    private Room getRoomByCode(String code) {
        return rooms.findByCode(code.toUpperCase().trim())
                .orElseThrow(() -> new NotFoundException("room not found"));
    }
}