package com.scribl.drawing;

import java.util.List;

/**
 * Incremental drawing payload for a poll. {@code sinceSeq} echoes the client's
 * high-water mark; {@code points} are the events strictly newer than it, in seq
 * order, so the client appends them to its replay and advances its watermark.
 */
public record RoundDrawingDto(
        int roundNumber,
        long sinceSeq,
        List<DrawingPointDto> points
) {
}