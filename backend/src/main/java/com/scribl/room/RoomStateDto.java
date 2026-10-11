package com.scribl.room;

import com.scribl.player.PlayerDto;

import java.util.List;

/** Pollable snapshot of a room. Deliberately mirrors the room + players tables;
 * round details and the word are added in the next milestone. */
public record RoomStateDto(
        String code,
        RoomStatus status,
        int totalRounds,
        int currentRoundNumber,
        Long hostPlayerId,
        List<PlayerDto> players
) {
}