package com.scribl.drawing;

import com.scribl.common.BadRequestException;
import com.scribl.common.ForbiddenException;
import com.scribl.common.NotFoundException;
import com.scribl.common.UnauthorizedException;
import com.scribl.game.Round;
import com.scribl.game.RoundRepository;
import com.scribl.player.Player;
import com.scribl.room.Room;
import com.scribl.room.RoomRepository;
import com.scribl.room.RoomStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Append-only drawing ingest and incremental retrieval.
 *
 * Writes are drawer-only and only while a round is active. The room is locked
 * (same pessimistic lock the round sweeper uses) so seq assignment (max+1 per
 * round) is collision-free without paying for a second index — and the
 * iterating (round_id, seq) read path stays unindexed as the V1 baseline.
 * Reads are open to any room member so all clients can replay the canvas.
 */
@Service
public class DrawingService {

    private final RoomRepository rooms;
    private final RoundRepository rounds;
    private final DrawingPointRepository points;

    public DrawingService(RoomRepository rooms, RoundRepository rounds, DrawingPointRepository points) {
        this.rooms = rooms;
        this.rounds = rounds;
        this.points = points;
    }

    @Transactional
    public DrawingAppendedDto append(String code, AppendDrawingRequest request, Player drawer) {
        Room room = rooms.findByCodeForUpdate(code.toUpperCase().trim())
                .orElseThrow(() -> new NotFoundException("room not found"));

        if (room.getStatus() != RoomStatus.IN_PROGRESS) {
            throw new ForbiddenException("drawing is only allowed during an active round");
        }
        if (room.getCurrentDrawerId() == null || !room.getCurrentDrawerId().equals(drawer.getId())) {
            throw new ForbiddenException("only the current drawer can draw");
        }
        DrawingAction action = actionOf(request.action());
        if (action == DrawingAction.POINT) {
            requirePointGeometry(request);
        }

        Round round = rounds.findByRoomIdAndRoundNumber(room.getId(), room.getCurrentRoundNumber())
                .orElseThrow(() -> new IllegalStateException("active round row missing"));

        long seq = points.maxSeq(round.getId()) + 1;
        points.save(new DrawingPoint(room.getId(), round.getId(), drawer.getId(), seq, action,
                request.x(), request.y(), request.color(), request.size(), request.tool()));
        return new DrawingAppendedDto(seq);
    }

    @Transactional(readOnly = true)
    public RoundDrawingDto fetch(String code, Player member, Integer roundNumber, Long sinceSeq) {
        Room room = getRoomByCode(code);
        if (!room.getId().equals(member.getRoomId())) {
            throw new UnauthorizedException("you are not a member of this room");
        }

        int targetRound = roundNumber == null ? room.getCurrentRoundNumber() : roundNumber;
        Round round = rounds.findByRoomIdAndRoundNumber(room.getId(), targetRound)
                .orElseThrow(() -> new NotFoundException("round not found"));

        long highWater = sinceSeq == null ? 0 : sinceSeq;
        List<DrawingPointDto> events = points.findByRoundIdAndSeqGreaterThanOrderBySeqAsc(round.getId(), highWater)
                .stream()
                .map(DrawingPointDto::from)
                .toList();
        return new RoundDrawingDto(round.getRoundNumber(), highWater, events);
    }

    private DrawingAction actionOf(String raw) {
        try {
            return DrawingAction.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("action must be POINT or CLEAR");
        }
    }

    private void requirePointGeometry(AppendDrawingRequest request) {
        if (request.x() == null || request.y() == null
                || request.color() == null || request.color().isBlank()
                || request.size() == null || request.tool() == null || request.tool().isBlank()) {
            throw new BadRequestException("POINT requires x, y, color, size and tool");
        }
    }

    private Room getRoomByCode(String code) {
        return rooms.findByCode(code.toUpperCase().trim())
                .orElseThrow(() -> new NotFoundException("room not found"));
    }
}