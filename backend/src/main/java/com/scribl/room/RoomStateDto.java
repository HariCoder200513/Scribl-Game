package com.scribl.room;

import com.scribl.player.PlayerDto;

import java.time.Instant;
import java.util.List;

/**
 * Pollable snapshot of a room. The current word is only exposed to the current
 * drawer; every other member sees null. Timers are computed from the
 * server-authoritative round deadline.
 */
public record RoomStateDto(
        String code,
        RoomStatus status,
        int totalRounds,
        int currentRoundNumber,
        Long hostPlayerId,
        Long currentDrawerId,
        Instant roundEndsAt,
        Long secondsRemaining,
        String currentWord,
        List<PlayerDto> players
) {
}