package com.liftsync.config;

import com.liftsync.config.i18n.MessageResolver;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.liftsync.dto.response.common.ErrorResponse;
import com.liftsync.dto.response.common.MessageListResponse;
import com.liftsync.dto.response.common.ValidationErrorsResponse;
import com.liftsync.exception.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageResolver messageResolver;

    public GlobalExceptionHandler(MessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    /**
     * Single entry point for every application exception. The HTTP status travels
     * with the exception ({@link ApiException#getStatus()}), so domain and semantic
     * exceptions (NotFoundException, ConflictException, BadRequestException, ...) are all
     * mapped here instead of needing one handler each.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return buildError(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(MessageListException.class)
    public ResponseEntity<MessageListResponse> handleMessageList(MessageListException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageListResponse(ex.messages()));
    }

    @ExceptionHandler(ValidationErrorsException.class)
    public ResponseEntity<ValidationErrorsResponse> handleValidationErrors(ValidationErrorsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ValidationErrorsResponse(ex.errors()));
    }

    /**
     * Safety net for {@link ResponseStatusException}s raised by Spring itself
     * (application code now throws {@link ApiException} subclasses instead).
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : "Request failed";
        return buildError(ex.getStatusCode(), message);
    }

    /**
     * Safety net for JPA {@link EntityNotFoundException}s (e.g. raised by lazy loading
     * or {@code getReference}). Application code should prefer {@link NotFoundException},
     * but this guarantees a 404 instead of falling through to the generic 500 catch-all.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
        String message = messageResolver.getMessage("error.optimistic.lock");
        return buildError(HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, messageResolver.getMessage("error.internal"));
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatusCode status, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(message));
    }

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbort(ClientAbortException ex){
        log.debug("Client aborted the request: {}", ex.getMessage());
    }

}
