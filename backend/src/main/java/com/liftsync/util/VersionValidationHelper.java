package com.liftsync.util;

import com.liftsync.config.i18n.MessageResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import com.liftsync.exception.BadRequestException;
import com.liftsync.exception.ConflictException;

/**
 * Helper class for validating version numbers in API requests for concurrency control.
 * It checks if the provided version is not null and matches the current version.
 * If validation fails, it throws appropriate exceptions with localized messages.
 */
@Component
public class VersionValidationHelper {

    private static final String DEFAULT_EMPTY_MESSAGE_KEY = "error.version.empty";
    private static final String DEFAULT_CONFLICT_MESSAGE_KEY = "error.version.conflict";

    private final MessageResolver messageResolver;

    public VersionValidationHelper(MessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    public void validateVersion(Number requestVersion, Number currentVersion) {
        validateVersion(requestVersion, currentVersion, DEFAULT_EMPTY_MESSAGE_KEY, DEFAULT_CONFLICT_MESSAGE_KEY);
    }

    public void validateVersion(Number requestVersion,
                                Number currentVersion,
                                String emptyMessageKey,
                                String conflictMessageKey) {
        if (requestVersion == null) {
            throw new BadRequestException(messageResolver.getMessage(emptyMessageKey));
        }

        if (currentVersion == null || requestVersion.longValue() != currentVersion.longValue()) {
            throw new ConflictException(messageResolver.getMessage(conflictMessageKey));
        }
    }
}

