package com.liftsync.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for every application exception that maps to a concrete HTTP response.
 * The status travels with the exception, so {@code GlobalExceptionHandler} can map
 * any subclass with a single handler reading {@link #getStatus()} instead of one
 * handler per type.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    protected ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
