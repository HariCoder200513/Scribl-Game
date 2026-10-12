package com.scribl.guessing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuessRequest(
        @NotBlank(message = "text is required")
        @Size(max = 255, message = "text must be at most 255 characters")
        String text
) {
}