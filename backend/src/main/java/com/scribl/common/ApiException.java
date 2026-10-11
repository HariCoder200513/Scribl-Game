package com.scribl.common;

import org.springframework.http.HttpStatus;

/**
 * Base class for expected, client-facing failures. Carries the HTTP status
 * that should be returned to the caller; the global exception handler maps it
 * to an {@link ApiError} body.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}