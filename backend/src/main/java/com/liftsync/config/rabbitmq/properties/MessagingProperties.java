package com.liftsync.config.rabbitmq.properties;

public interface MessagingProperties {
    String exchange();
    String queue();
    String routingKey();
    String retryExchange();
    String retryQueue();
    String retryRoutingKey();
    String dlqExchange();
    String dlqQueue();
    String dlqRoutingKey();
    long retryDelayMs();
}
