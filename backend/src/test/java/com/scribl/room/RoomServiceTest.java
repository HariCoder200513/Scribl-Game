package com.scribl.room;

import com.scribl.common.ConflictException;
import com.scribl.common.UnauthorizedException;
import com.scribl.player.Player;
import com.scribl.player.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    RoomRepository rooms;
    @Mock
    PlayerRepository players;
    @Mock
    RoomCodeGenerator codeGenerator;
    @InjectMocks
    RoomService roomService;

    private void stubGeneratedIds() {
        when(rooms.save(any(Room.class))).thenAnswer(inv -> {
            Room r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(10L);
            }
            return r;
        });
        when(players.save(any(Player.class))).thenAnswer(inv -> {
            Player p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(5L);
            }
            return p;
        });
    }

    private Room lobbyRoom(long id) {
        Room room = new Room();
        room.setId(id);
        room.setCode("ABCDEF");
        room.setStatus(RoomStatus.LOBBY);
        return room;
    }

    @Test
    void createRoom_retriesWhenCodeAlreadyExists() {
        stubGeneratedIds();
        when(codeGenerator.generate()).thenReturn("ABCDEF", "ABCDEG");
        when(rooms.existsByCode("ABCDEF")).thenReturn(true);
        when(rooms.existsByCode("ABCDEG")).thenReturn(false);

        RoomSessionDto session = roomService.createRoom("alice", 5);

        assertThat(session.roomCode()).isEqualTo("ABCDEG");
        assertThat(session.playerId()).isEqualTo(5L);
        verify(rooms, times(2)).existsByCode(anyString());
    }

    @Test
    void createRoom_defaultsToThreeRoundsWhenUnspecified() {
        stubGeneratedIds();
        when(codeGenerator.generate()).thenReturn("ABCDEF");
        when(rooms.existsByCode("ABCDEF")).thenReturn(false);

        roomService.createRoom("alice", null);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(rooms, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getTotalRounds()).isEqualTo(3);
    }

    @Test
    void joinRoom_rejectsNonLobbyRoom() {
        Room room = lobbyRoom(1L);
        room.setStatus(RoomStatus.IN_PROGRESS);
        when(rooms.findByCode("ABCDEF")).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.joinRoom("abcdef", "bob"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void joinRoom_rejectsWhenRoomIsFull() {
        when(rooms.findByCode("ABCDEF")).thenReturn(Optional.of(lobbyRoom(1L)));
        when(players.countByRoomId(1L)).thenReturn((long) RoomService.MAX_PLAYERS);

        assertThatThrownBy(() -> roomService.joinRoom("abcdef", "bob"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void joinRoom_rejectsDuplicateName() {
        when(rooms.findByCode("ABCDEF")).thenReturn(Optional.of(lobbyRoom(1L)));
        when(players.countByRoomId(1L)).thenReturn(3L);
        when(players.existsByRoomIdAndName(1L, "bob")).thenReturn(true);

        assertThatThrownBy(() -> roomService.joinRoom("abcdef", "bob"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void getState_rejectsPlayerFromAnotherRoom() {
        when(rooms.findByCode("ABCDEF")).thenReturn(Optional.of(lobbyRoom(1L)));
        Player outsider = new Player();
        outsider.setRoomId(2L);

        assertThatThrownBy(() -> roomService.getState("ABCDEF", outsider))
                .isInstanceOf(UnauthorizedException.class);
    }
}