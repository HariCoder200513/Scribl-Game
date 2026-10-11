package com.scribl.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRoomRequest(
        @NotBlank(message = "playerName is required")
        @Size(max = 32, message = "playerName must be at most 32 characters")
        String playerName
) {
}