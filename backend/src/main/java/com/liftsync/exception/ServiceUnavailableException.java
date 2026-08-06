package com.liftsync.exception;

import org.springframework.http.HttpStatus;

/** Upstream dependency unavailable (e.g. email/storage provider down). Maps to HTTP 503. */
public class ServiceUnavailableException extends ApiException {
    public ServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
