package com.liftsync.exception;

import org.springframework.http.HttpStatus;

/** Unexpected server-side failure surfaced deliberately. Maps to HTTP 500. */
public class InternalServerException extends ApiException {
    public InternalServerException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public InternalServerException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
