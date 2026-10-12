package com.scribl.room;

import com.scribl.player.PlayerDto;

import java.util.List;

/** Standings for a room; ties keep join order. Available to room members. */
public record LeaderboardDto(String code, RoomStatus status, List<PlayerDto> standings) {
}