package com.liftsync.exception;

import org.springframework.http.HttpStatus;

/** Request conflicts with current state (e.g. concurrent edit, duplicate). Maps to HTTP 409. */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
