package com.liftsync.exception;

import org.springframework.http.HttpStatus;

/** Invalid request / failed business validation. Maps to HTTP 400. */
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
