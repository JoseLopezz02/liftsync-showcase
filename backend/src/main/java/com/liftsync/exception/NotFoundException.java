package com.liftsync.exception;

import org.springframework.http.HttpStatus;

/** Resource not found. Maps to HTTP 404. */
public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
