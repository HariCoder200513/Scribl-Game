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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuessServiceTest {

    private static final long ROOM_ID = 1L;
    private static final long ROUND_ID = 5L;
    private static final long DRAWER_ID = 10L;
    private static final long GUESSER_ID = 20L;

    @Mock
    RoomRepository rooms;
    @Mock
    PlayerRepository players;
    @Mock
    RoundRepository rounds;
    @Mock
    GuessRepository guesses;
    @Mock
    RoundService roundService;

    GuessService guessService;

    @BeforeEach
    void setUp() {
        guessService = new GuessService(rooms, players, rounds, guesses, roundService);
    }

    private Room inProgressRoom() {
        Room room = new Room();
        room.setId(ROOM_ID);
        room.setCode("ABCDEF");
        room.setStatus(RoomStatus.IN_PROGRESS);
        room.setCurrentRoundNumber(1);
        room.setCurrentDrawerId(DRAWER_ID);
        room.setCurrentWord("cat");
        room.setTotalRounds(3);
        room.setRoundEndsAt(Instant.now().plusSeconds(30));
        return room;
    }

    private Round activeRound() {
        Round round = new Round(ROOM_ID, 1, DRAWER_ID, "cat", RoundStatus.ACTIVE, Instant.now().minusSeconds(5));
        round.setId(ROUND_ID);
        return round;
    }

    private Player player(long id, int score) {
        Player p = new Player();
        p.setId(id);
        p.setRoomId(ROOM_ID);
        p.setScore(score);
        return p;
    }

    @Test
    void correctGuess_scoresGuesserAndDrawer_andEndsTheRound() {
        Room room = inProgressRoom();
        Player drawer = player(DRAWER_ID, 0);
        Player guesser = player(GUESSER_ID, 0);

        when(rooms.findByCodeForUpdate("ABCDEF")).thenReturn(Optional.of(room));
        when(rounds.findByRoomIdAndRoundNumber(ROOM_ID, 1)).thenReturn(Optional.of(activeRound()));
        when(players.findById(DRAWER_ID)).thenReturn(Optional.of(drawer));
        when(guesses.save(any(Guess.class))).thenAnswer(inv -> inv.getArgument(0));
        when(players.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rooms.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        GuessResultDto result = guessService.guess("abcdef", new GuessRequest("cat"), guesser);

        assertThat(result.correct()).isTrue();
        assertThat(result.awardedPoints()).isBetween(100L, 130L);
        assertThat(guesser.getScore()).isEqualTo((int) result.awardedPoints());
        assertThat(drawer.getScore()).isEqualTo(GuessService.DRAWER_BONUS_POINTS);
        verify(roundService).completeRound(room);

        ArgumentCaptor<Guess> captor = ArgumentCaptor.forClass(Guess.class);
        verify(guesses).save(captor.capture());
        Guess stored = captor.getValue();
        assertThat(stored.isCorrect()).isTrue();
        assertThat(stored.getText()).isEqualTo("cat");
        assertThat(stored.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(stored.getRoundId()).isEqualTo(ROUND_ID);
        assertThat(stored.getPlayerId()).isEqualTo(GUESSER_ID);
    }

    @Test
    void wrongGuess_recordsTheAttempt_andScoresNothing() {
        when(rooms.findByCodeForUpdate("ABCDEF")).thenReturn(Optional.of(inProgressRoom()));
        when(rounds.findByRoomIdAndRoundNumber(ROOM_ID, 1)).thenReturn(Optional.of(activeRound()));
        when(guesses.save(any(Guess.class))).thenAnswer(inv -> inv.getArgument(0));
        Player guesser = player(GUESSER_ID, 0);

        GuessResultDto result = guessService.guess("ABCDEF", new GuessRequest("dog"), guesser);

        assertThat(result.correct()).isFalse();
        assertThat(result.awardedPoints()).isZero();
        assertThat(guesser.getScore()).isZero();
        verify(roundService, never()).completeRound(any());
        verify(players, never()).save(any());
    }

    @Test
    void guess_isCaseInsensitiveAndTrimmed() {
        when(rooms.findByCodeForUpdate("ABCDEF")).thenReturn(Optional.of(inProgressRoom()));
        when(rounds.findByRoomIdAndRoundNumber(ROOM_ID, 1)).thenReturn(Optional.of(activeRound()));
        when(players.findById(DRAWER_ID)).thenReturn(Optional.empty());
        when(guesses.save(any(Guess.class))).thenAnswer(inv -> inv.getArgument(0));
        when(players.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rooms.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        GuessResultDto result = guessService.guess("ABCDEF", new GuessRequest("  CaT "), player(GUESSER_ID, 0));

        assertThat(result.correct()).isTrue();
    }

    @Test
    void drawerCannotGuess() {
        when(rooms.findByCodeForUpdate("ABCDEF")).thenReturn(Optional.of(inProgressRoom()));
        assertThatThrownBy(() -> guessService.guess("ABCDEF", new GuessRequest("cat"), player(DRAWER_ID, 0)))
                .isInstanceOf(ForbiddenException.class);
        verify(guesses, never()).save(any());
        verify(roundService, never()).completeRound(any());
    }

    @Test
    void guessingNotAllowedOutsideAnActiveRound() {
        Room room = inProgressRoom();
        room.setStatus(RoomStatus.FINISHED);
        when(rooms.findByCodeForUpdate("ABCDEF")).thenReturn(Optional.of(room));
        assertThatThrownBy(() -> guessService.guess("ABCDEF", new GuessRequest("cat"), player(GUESSER_ID, 0)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void guessingRejectedWhenRoundAlreadyEnded() {
        Round ended = activeRound();
        ended.setStatus(RoundStatus.ENDED);
        when(rooms.findByCodeForUpdate("ABCDEF")).thenReturn(Optional.of(inProgressRoom()));
        when(rounds.findByRoomIdAndRoundNumber(ROOM_ID, 1)).thenReturn(Optional.of(ended));
        assertThatThrownBy(() -> guessService.guess("ABCDEF", new GuessRequest("cat"), player(GUESSER_ID, 0)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void guessingByNonMemberIsUnauthorized() {
        when(rooms.findByCodeForUpdate("ABCDEF")).thenReturn(Optional.of(inProgressRoom()));
        Player outsider = new Player();
        outsider.setId(99L);
        outsider.setRoomId(999L);
        assertThatThrownBy(() -> guessService.guess("ABCDEF", new GuessRequest("cat"), outsider))
                .isInstanceOf(UnauthorizedException.class);
        verify(rounds, never()).findByRoomIdAndRoundNumber(anyLong(), anyInt());
    }

    @Test
    void guessingUnknownRoomIsNotFound() {
        when(rooms.findByCodeForUpdate("NOPE99")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> guessService.guess("NOPE99", new GuessRequest("cat"), player(GUESSER_ID, 0)))
                .isInstanceOf(NotFoundException.class);
    }
}