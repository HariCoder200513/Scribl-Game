package com.scribl.room;

/** Credentials handed to a client on create/join. The token must be kept by the
 * client and resent on every authenticated request. */
public record RoomSessionDto(String roomCode, Long playerId, String playerToken) {
}