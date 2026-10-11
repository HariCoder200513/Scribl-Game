package com.scribl.room;

public enum RoomStatus {
    /** Players may still join; the game has not started. */
    LOBBY,
    /** At least one round is in progress. */
    IN_PROGRESS,
    /** All rounds have been played; final leaderboard available. */
    FINISHED
}