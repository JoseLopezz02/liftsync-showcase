package com.liftsync.messaging.email.handler;

import com.liftsync.messaging.email.EmailMessage;
import com.liftsync.messaging.email.EmailType;

public interface EmailMessageHandler {

    EmailType getSupportedEmailType();

    void handle(EmailMessage emailMessage);
}
