package com.scribl.drawing;

import jakarta.validation.constraints.NotBlank;

/**
 * A drawing event from the current drawer. {@code POINT} requires geometry and
 * style; {@code CLEAR} ignores them. Cross-field validation happens in the
 * service because the requirements differ per action.
 */
public record AppendDrawingRequest(
        @NotBlank(message = "action is required") String action,
        Double x,
        Double y,
        String color,
        Integer size,
        String tool
) {
}