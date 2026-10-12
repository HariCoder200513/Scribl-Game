package com.scribl.drawing;

/** The seq assigned to an accepted event; the client's new high-water mark. */
public record DrawingAppendedDto(long seq) {
}