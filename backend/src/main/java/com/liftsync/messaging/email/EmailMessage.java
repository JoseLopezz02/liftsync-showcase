package com.liftsync.messaging.email;

import java.util.Map;

public record EmailMessage(
        String to,
        EmailType type,
        String locale,
        Map<String, String> variables
) {}
