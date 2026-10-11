package com.scribl.room;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank(message = "playerName is required")
        @Size(max = 32, message = "playerName must be at most 32 characters")
        String playerName,

        @Min(value = 1, message = "totalRounds must be at least 1")
        @Max(value = 10, message = "totalRounds must be at most 10")
        Integer totalRounds
) {
}