package com.liftsync.config.rabbitmq.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging.email")
public record EmailMessagingProperties (
        String queue,
        String exchange,
        String routingKey,
        String retryQueue,
        String retryExchange,
        String retryRoutingKey,
        String dlqQueue,
        String dlqExchange,
        String dlqRoutingKey,
        String retryCountHeader,
        String failureReasonHeader,
        int maxRetries,
        long retryDelayMs) implements MessagingProperties {
}
