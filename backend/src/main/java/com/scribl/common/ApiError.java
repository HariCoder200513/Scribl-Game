package com.scribl.common;

import java.time.Instant;

/** Machine-readable error body returned for expected API failures. */
public record ApiError(Instant timestamp, int status, String error, String message) {
}