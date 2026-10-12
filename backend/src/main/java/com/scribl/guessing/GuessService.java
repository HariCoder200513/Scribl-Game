package com.scribl.guessing;

import com.scribl.common.ConflictException;
import com.scribl.common.ForbiddenException;
import com.scribl.common.NotFoundException;
import com.scribl.common.UnauthorizedException;
import com.scribl.game.Round;
import com.scribl.game.RoundRepository;
import com.scribl.game.RoundService;
import com.scribl.game.RoundStatus;
import com.scribl.player.Player;
import com.scribl.player.PlayerRepository;
import com.scribl.room.Room;
import com.scribl.room.RoomRepository;
import com.scribl.room.RoomStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Guess submission and scoring.
 *
 * A correct guess is worth {@value #GUESS_BASE_POINTS} plus the seconds left on
 * the round clock (faster guesses score more); the drawer earns a flat
 * {@value #DRAWER_BONUS_POINTS} whenever someone solves their drawing. A correct
 * guess also ends the round immediately and advances the game through the same
 * guarded transition the timer sweep uses.
 *
 * V1 simplification: there is no separate "round over" interlude before the next
 * round begins, and scores are written in the same transaction as the guess.
 */
@Service
public class GuessService {

    public static final int GUESS_BASE_POINTS = 100;
    public static final int DRAWER_BONUS_POINTS = 50;

    private final RoomRepository rooms;
    private final PlayerRepository players;
    private final RoundRepository rounds;
    private final GuessRepository guesses;
    private final RoundService roundService;

    public GuessService(RoomRepository rooms,
                        PlayerRepository players,
                        RoundRepository rounds,
                        GuessRepository guesses,
                        RoundService roundService) {
        this.rooms = rooms;
        this.players = players;
        this.rounds = rounds;
        this.guesses = guesses;
        this.roundService = roundService;
    }

    @Transactional
    public GuessResultDto guess(String code, GuessRequest request, Player guesser) {
        Room room = rooms.findByCodeForUpdate(code.toUpperCase().trim())
                .orElseThrow(() -> new NotFoundException("room not found"));

        if (!room.getId().equals(guesser.getRoomId())) {
            throw new UnauthorizedException("you are not a member of this room");
        }
        if (room.getStatus() != RoomStatus.IN_PROGRESS) {
            throw new ForbiddenException("guessing is only allowed during an active round");
        }
        if (room.getCurrentDrawerId() != null && room.getCurrentDrawerId().equals(guesser.getId())) {
            throw new ForbiddenException("the drawer cannot guess");
        }

        Round round = rounds.findByRoomIdAndRoundNumber(room.getId(), room.getCurrentRoundNumber())
                .orElseThrow(() -> new IllegalStateException("active round row missing"));
        if (round.getStatus() != RoundStatus.ACTIVE) {
            throw new ConflictException("the round is already over");
        }

        String text = request.text().trim();
        boolean correct = text.equalsIgnoreCase(round.getWord());
        guesses.save(new Guess(room.getId(), round.getId(), guesser.getId(), text, correct));

        if (!correct) {
            return new GuessResultDto(false, 0);
        }

        long awarded = GUESS_BASE_POINTS + secondsRemaining(room);
        guesser.setScore(guesser.getScore() + Math.toIntExact(awarded));
        players.save(guesser);

        players.findById(room.getCurrentDrawerId()).ifPresent(drawer -> {
            drawer.setScore(drawer.getScore() + DRAWER_BONUS_POINTS);
            players.save(drawer);
        });

        roundService.completeRound(room);
        rooms.save(room);
        return new GuessResultDto(true, awarded);
    }

    private long secondsRemaining(Room room) {
        if (room.getRoundEndsAt() == null) {
            return 0;
        }
        return Math.max(0, Duration.between(Instant.now(), room.getRoundEndsAt()).getSeconds());
    }
}