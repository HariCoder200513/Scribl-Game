package com.scribl.game;

import com.scribl.common.ConflictException;
import com.scribl.common.ForbiddenException;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundServiceTest {

    private static final long ROOM_ID = 1L;
    private static final long HOST_ID = 10L;
    private static final long OTHER_ID = 20L;

    @Mock
    RoomRepository rooms;
    @Mock
    PlayerRepository players;
    @Mock
    RoundRepository rounds;
    @Mock
    WordProvider wordProvider;

    RoundService roundService;

    @BeforeEach
    void setUp() {
        roundService = new RoundService(rooms, players, rounds, wordProvider, 60);
    }

    private Room lobbyRoom() {
        Room room = new Room();
        room.setId(ROOM_ID);
        room.setCode("ABCDEF");
        room.setStatus(RoomStatus.LOBBY);
        room.setHostPlayerId(HOST_ID);
        room.setTotalRounds(3);
        return room;
    }

    private Room inProgressRoom(int roundNumber, Instant roundEndsAt) {
        Room room = lobbyRoom();
        room.setStatus(RoomStatus.IN_PROGRESS);
        room.setCurrentRoundNumber(roundNumber);
        room.setCurrentDrawerId(HOST_ID);
        room.setCurrentWord("cat");
        room.setRoundEndsAt(roundEndsAt);
        return room;
    }

    private Player player(long id, String name) {
        Player p = new Player();
        p.setId(id);
        p.setRoomId(ROOM_ID);
        p.setName(name);
        return p;
    }

    private Player host() {
        return player(HOST_ID, "alice");
    }

    @Test
    void start_rejectsNonHost() {
        when(rooms.findByCode("ABCDEF")).thenReturn(Optional.of(lobbyRoom()));
        assertThatThrownBy(() -> roundService.start("ABCDEF", player(OTHER_ID, "bob")))
                .isInstanceOf(ForbiddenException.class);
        verify(rounds, never()).save(any());
    }

    @Test
    void start_rejectsRoomThatAlreadyStarted() {
        Room room = inProgressRoom(1, Instant.now().plusSeconds(60));
        when(rooms.findByCode("ABCDEF")).thenReturn(Optional.of(room));
        assertThatThrownBy(() -> roundService.start("ABCDEF", host()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void start_rejectsRoomWithSinglePlayer() {
        when(rooms.findByCode("ABCDEF")).thenReturn(Optional.of(lobbyRoom()));
        when(players.countByRoomId(ROOM_ID)).thenReturn(1L);
        assertThatThrownBy(() -> roundService.start("ABCDEF", host()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void start_beginsFirstRoundWithHostAsDrawer() {
        when(rooms.findByCode("ABCDEF")).thenReturn(Optional.of(lobbyRoom()));
        when(players.countByRoomId(ROOM_ID)).thenReturn(2L);
        when(players.findByRoomIdOrderByJoinedAtAsc(ROOM_ID))
                .thenReturn(List.of(player(HOST_ID, "alice"), player(OTHER_ID, "bob")));
        when(rounds.findWordsByRoomId(ROOM_ID)).thenReturn(List.of());
        when(wordProvider.pick(anySet())).thenReturn("cat");
        when(rooms.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rounds.save(any(Round.class))).thenAnswer(inv -> inv.getArgument(0));

        roundService.start("abcdef", host());

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(rooms).save(roomCaptor.capture());
        Room saved = roomCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
        assertThat(saved.getCurrentRoundNumber()).isEqualTo(1);
        assertThat(saved.getCurrentDrawerId()).isEqualTo(HOST_ID);
        assertThat(saved.getCurrentWord()).isEqualTo("cat");
        assertThat(saved.getRoundEndsAt()).isBetween(Instant.now(), Instant.now().plusSeconds(60));

        ArgumentCaptor<Round> roundCaptor = ArgumentCaptor.forClass(Round.class);
        verify(rounds).save(roundCaptor.capture());
        Round stored = roundCaptor.getValue();
        assertThat(stored.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(stored.getRoundNumber()).isEqualTo(1);
        assertThat(stored.getDrawerPlayerId()).isEqualTo(HOST_ID);
        assertThat(stored.getWord()).isEqualTo("cat");
        assertThat(stored.getStatus()).isEqualTo(RoundStatus.ACTIVE);
    }

    @Test
    void advance_transitionToNextRound() {
        Instant now = Instant.now();
        Room room = inProgressRoom(1, now.minusSeconds(10));
        Round activeRound = new Round(ROOM_ID, 1, HOST_ID, "cat", RoundStatus.ACTIVE, now.minusSeconds(70));

        when(rooms.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(rounds.findByRoomIdAndRoundNumber(ROOM_ID, 1)).thenReturn(Optional.of(activeRound));
        when(players.findByRoomIdOrderByJoinedAtAsc(ROOM_ID))
                .thenReturn(List.of(player(HOST_ID, "alice"), player(OTHER_ID, "bob")));
        when(rounds.findWordsByRoomId(ROOM_ID)).thenReturn(List.of("cat"));
        when(wordProvider.pick(anySet())).thenReturn("dog");
        when(rooms.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rounds.save(any(Round.class))).thenAnswer(inv -> inv.getArgument(0));

        roundService.advanceIfExpired(ROOM_ID);

        assertThat(activeRound.getStatus()).isEqualTo(RoundStatus.ENDED);
        assertThat(activeRound.getEndedAt()).isNotNull();

        assertThat(room.getCurrentRoundNumber()).isEqualTo(2);
        assertThat(room.getCurrentDrawerId()).isEqualTo(OTHER_ID);
        assertThat(room.getCurrentWord()).isEqualTo("dog");
        assertThat(room.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);

        ArgumentCaptor<Round> roundCaptor = ArgumentCaptor.forClass(Round.class);
        verify(rounds, times(2)).save(roundCaptor.capture());
        List<Round> savedRounds = roundCaptor.getAllValues();
        assertThat(savedRounds).hasSize(2);
        assertThat(savedRounds.get(0).getStatus()).isEqualTo(RoundStatus.ENDED);
        assertThat(savedRounds.get(0).getEndedAt()).isNotNull();

        Round next = savedRounds.get(1);
        assertThat(next.getRoundNumber()).isEqualTo(2);
        assertThat(next.getDrawerPlayerId()).isEqualTo(OTHER_ID);
        assertThat(next.getWord()).isEqualTo("dog");
        verify(wordProvider).pick(anySet());
    }

    @Test
    void advance_finishesAfterTheFinalRound() {
        Instant now = Instant.now();
        Room room = inProgressRoom(3, now.minusSeconds(10));
        room.setTotalRounds(3);
        Round lastRound = new Round(ROOM_ID, 3, HOST_ID, "zebra", RoundStatus.ACTIVE, now.minusSeconds(70));

        when(rooms.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(rounds.findByRoomIdAndRoundNumber(ROOM_ID, 3)).thenReturn(Optional.of(lastRound));
        when(rooms.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rounds.save(any(Round.class))).thenAnswer(inv -> inv.getArgument(0));

        roundService.advanceIfExpired(ROOM_ID);

        assertThat(lastRound.getStatus()).isEqualTo(RoundStatus.ENDED);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.FINISHED);
        assertThat(room.getCurrentDrawerId()).isNull();
        assertThat(room.getCurrentWord()).isNull();
        assertThat(room.getRoundEndsAt()).isNull();
        verify(players, never()).findByRoomIdOrderByJoinedAtAsc(anyLong());
    }

    @Test
    void advance_noopWhenRoundHasNotExpired() {
        Room room = inProgressRoom(1, Instant.now().plusSeconds(60));
        when(rooms.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));

        roundService.advanceIfExpired(ROOM_ID);

        verify(rounds, never()).findByRoomIdAndRoundNumber(anyLong(), anyInt());
        verify(rounds, never()).save(any());
        assertThat(room.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
    }
}