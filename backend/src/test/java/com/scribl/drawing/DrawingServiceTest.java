package com.scribl.drawing;

import com.scribl.common.BadRequestException;
import com.scribl.common.ForbiddenException;
import com.scribl.common.NotFoundException;
import com.scribl.common.UnauthorizedException;
import com.scribl.game.Round;
import com.scribl.game.RoundRepository;
import com.scribl.game.RoundStatus;
import com.scribl.player.Player;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DrawingServiceTest {

    private static final long ROOM_ID = 1L;
    private static final long ROUND_ID = 5L;
    private static final long DRAWER_ID = 10L;
    private static final long GUESSER_ID = 20L;

    @Mock
    RoomRepository rooms;
    @Mock
    RoundRepository rounds;
    @Mock
    DrawingPointRepository points;

    DrawingService drawingService;

    @BeforeEach
    void setUp() {
        drawingService = new DrawingService(rooms, rounds, points);
    }

    private Room inProgressRoom() {
        Room room = new Room();
        room.setId(ROOM_ID);
        room.setCode("ABCDEF");
        room.setStatus(RoomStatus.IN_PROGRESS);
        room.setCurrentRoundNumber(1);
        room.setCurrentDrawerId(DRAWER_ID);
        room.setCurrentWord("cat");
        room.setRoundEndsAt(Instant.now().plusSeconds(60));
        return room;
    }

    private Round activeRound() {
        Round round = new Round(ROOM_ID, 1, DRAWER_ID, "cat", RoundStatus.ACTIVE, Instant.now().minusSeconds(10));
        round.setId(ROUND_ID);
        return round;
    }

    private Player player(long id) {
        Player p = new Player();
        p.setId(id);
        p.setRoomId(ROOM_ID);
        return p;
    }

    private AppendDrawingRequest pointRequest() {
        return new AppendDrawingRequest("POINT", 12.5, 8.0, "#FF0000", 4, "BRUSH");
    }

    @Test
    void append_assignsNextSeqPerRound() {
        when(rooms.findByCodeForUpdate("ABCDEF")).thenReturn(Optional.of(inProgressRoom()));
        when(rounds.findByRoomIdAndRoundNumber(ROOM_ID, 1)).thenReturn(Optional.of(activeRound()));
        when(points.maxSeq(ROUND_ID)).thenReturn(41L);

        DrawingAppendedDto result = drawingService.append("ABCDEF", pointRequest(), player(DRAWER_ID));

        assertThat(result.seq()).isEqualTo(42L);
        ArgumentCaptor<DrawingPoint> captor = ArgumentCaptor.forClass(DrawingPoint.class);
        verify(points).save(captor.capture());
        DrawingPoint saved = captor.getValue();
        assertThat(saved.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(saved.getRoundId()).isEqualTo(ROUND_ID);
        assertThat(saved.getSeq()).isEqualTo(42L);
        assertThat(saved.getAction()).isEqualTo(DrawingAction.POINT);
        assertThat(saved.getX()).isEqualTo(12.5);
        assertThat(saved.getSize()).isEqualTo(4);
        assertThat(saved.getPlayerId()).isEqualTo(DRAWER_ID);
    }

    @Test
    void append_startsSeqsAtOneForAnEmptyRound() {
        when(rooms.findByCodeForUpdate("ABCDEF")).thenReturn(Optional.of(inProgressRoom()));
        when(rounds.findByRoomIdAndRoundNumber(ROOM_ID, 1)).thenReturn(Optional.of(activeRound()));
        when(points.maxSeq(ROUND_ID)).thenReturn(0L);

        DrawingAppendedDto result = drawingService.append("ABCDEF", pointRequest(), player(DRAWER_ID));

        assertThat(result.seq()).isEqualTo(1L);
    }

    @Test
    void append_clearCarriesNoGeometry() {
        when(rooms.findByCodeForUpdate("ABCDEF")).thenReturn(Optional.of(inProgressRoom()));
        when(rounds.findByRoomIdAndRoundNumber(ROOM_ID, 1)).thenReturn(Optional.of(activeRound()));
        when(points.maxSeq(ROUND_ID)).thenReturn(3L);

        drawingService.append("ABCDEF", new AppendDrawingRequest("CLEAR", null, null, null, null, null),
                player(DRAWER_ID));

        ArgumentCaptor<DrawingPoint> captor = ArgumentCaptor.forClass(DrawingPoint.class);
        verify(points).save(captor.capture());
        DrawingPoint saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo(DrawingAction.CLEAR);
        assertThat(saved.getSeq()).isEqualTo(4L);
        assertThat(saved.getX()).isNull();
        assertThat(saved.getColor()).isNull();
        assertThat(saved.getTool()).isNull();
    }

    @Test
    void append_rejectsNonDrawer() {
        when(rooms.findByCodeForUpdate("ABCDEF")).thenReturn(Optional.of(inProgressRoom()));
        assertThatThrownBy(() -> drawingService.append("ABCDEF", pointRequest(), player(GUESSER_ID)))
                .isInstanceOf(ForbiddenException.class);
        verify(points, never()).save(any());
    }

    @Test
    void append_rejectsOutsideAnActiveRound() {
        Room room = inProgressRoom();
        room.setStatus(RoomStatus.FINISHED);
        when(rooms.findByCodeForUpdate("ABCDEF")).thenReturn(Optional.of(room));
        assertThatThrownBy(() -> drawingService.append("ABCDEF", pointRequest(), player(DRAWER_ID)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void append_rejectsPointWithoutGeometry() {
        when(rooms.findByCodeForUpdate("ABCDEF")).thenReturn(Optional.of(inProgressRoom()));
        assertThatThrownBy(() -> drawingService.append("ABCDEF", new AppendDrawingRequest("POINT", null, null,
                null, 3, "BRUSH"), player(DRAWER_ID)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void append_rejectsUnknownAction() {
        when(rooms.findByCodeForUpdate("ABCDEF")).thenReturn(Optional.of(inProgressRoom()));
        assertThatThrownBy(() -> drawingService.append("ABCDEF", new AppendDrawingRequest("PAN", 1.0, 1.0,
                "#000000", 2, "BRUSH"), player(DRAWER_ID)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void append_unknownRoomIsNotFound() {
        when(rooms.findByCodeForUpdate("NOPE99")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> drawingService.append("NOPE99", pointRequest(), player(DRAWER_ID)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void fetch_returnsOnlyEventsNewerThanTheHighWaterMark() {
        Room room = inProgressRoom();
        when(rooms.findByCode("ABCDEF")).thenReturn(Optional.of(room));
        when(rounds.findByRoomIdAndRoundNumber(ROOM_ID, 1)).thenReturn(Optional.of(activeRound()));

        DrawingPoint p1 = new DrawingPoint(ROOM_ID, ROUND_ID, DRAWER_ID, 1, DrawingAction.POINT, 1.0, 1.0, "#000000", 2, "BRUSH");
        DrawingPoint p7 = new DrawingPoint(ROOM_ID, ROUND_ID, DRAWER_ID, 7, DrawingAction.CLEAR, null, null, null, null, null);
        when(points.findByRoundIdAndSeqGreaterThanOrderBySeqAsc(ROUND_ID, 5L)).thenReturn(List.of(p7));

        RoundDrawingDto result = drawingService.fetch("ABCDEF", player(GUESSER_ID), null, 5L);

        assertThat(result.roundNumber()).isEqualTo(1);
        assertThat(result.sinceSeq()).isEqualTo(5L);
        assertThat(result.points()).hasSize(1);
        DrawingPointDto event = result.points().get(0);
        assertThat(event.seq()).isEqualTo(7L);
        assertThat(event.action()).isEqualTo("CLEAR");
        assertThat(event.x()).isNull();
    }

    @Test
    void fetch_defaultsSinceSeqToZero_andHonorsRoundNumber() {
        when(rooms.findByCode("ABCDEF")).thenReturn(Optional.of(inProgressRoom()));
        Round round2 = new Round(ROOM_ID, 2, DRAWER_ID, "dog", RoundStatus.ACTIVE, Instant.now());
        round2.setId(ROUND_ID);
        when(rounds.findByRoomIdAndRoundNumber(ROOM_ID, 2)).thenReturn(Optional.of(round2));
        when(points.findByRoundIdAndSeqGreaterThanOrderBySeqAsc(ROUND_ID, 0L)).thenReturn(List.of());

        RoundDrawingDto result = drawingService.fetch("ABCDEF", player(GUESSER_ID), 2, null);

        assertThat(result.roundNumber()).isEqualTo(2);
        assertThat(result.sinceSeq()).isEqualTo(0L);
        assertThat(result.points()).isEmpty();
    }

    @Test
    void fetch_rejectsNonMember() {
        when(rooms.findByCode("ABCDEF")).thenReturn(Optional.of(inProgressRoom()));
        Player outsider = new Player();
        outsider.setId(99L);
        outsider.setRoomId(999L);
        assertThatThrownBy(() -> drawingService.fetch("ABCDEF", outsider, null, null))
                .isInstanceOf(UnauthorizedException.class);
        verify(rounds, never()).findByRoomIdAndRoundNumber(any(), anyInt());
    }

    @Test
    void fetch_unknownRoundIsNotFound() {
        when(rooms.findByCode("ABCDEF")).thenReturn(Optional.of(inProgressRoom()));
        when(rounds.findByRoomIdAndRoundNumber(eq(ROOM_ID), eq(9))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> drawingService.fetch("ABCDEF", player(GUESSER_ID), 9, null))
                .isInstanceOf(NotFoundException.class);
    }
}