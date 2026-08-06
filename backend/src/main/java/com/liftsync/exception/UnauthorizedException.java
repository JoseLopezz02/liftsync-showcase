package com.liftsync.exception;

import org.springframework.http.HttpStatus;

/** Authentication missing or invalid. Maps to HTTP 401. */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
