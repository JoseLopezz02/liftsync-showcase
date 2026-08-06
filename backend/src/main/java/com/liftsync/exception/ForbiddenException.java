package com.liftsync.exception;

import org.springframework.http.HttpStatus;

/** Authenticated but not allowed to perform the action. Maps to HTTP 403. */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
