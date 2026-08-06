package com.liftsync.messaging.common;

import com.liftsync.config.rabbitmq.properties.MessagingProperties;
import org.springframework.amqp.core.*;

import java.util.Map;

public final class MessagingTopology {

    public static Declarables declarables(MessagingProperties properties) {
        var exchange = new DirectExchange(properties.exchange());
        var retryExchange = new DirectExchange(properties.retryExchange());
        var dlqExchange = new DirectExchange(properties.dlqExchange());
        var queue = new Queue(properties.queue());
        var retryQueue = QueueBuilder.durable(properties.retryQueue())
                .withArguments(Map.of(
                        "x-message-ttl", properties.retryDelayMs(),
                        "x-dead-letter-exchange", properties.exchange(),
                        "x-dead-letter-routing-key", properties.routingKey()
                ))
                .build();

        var dlqQueue = new Queue(properties.dlqQueue(), true);
        var bindQueue = BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(properties.routingKey());

        var bindRetryQueue = BindingBuilder
                .bind(retryQueue)
                .to(retryExchange)
                .with(properties.retryRoutingKey());

        var bindDlqQueue = BindingBuilder
                .bind(dlqQueue)
                .to(dlqExchange)
                .with(properties.dlqRoutingKey());

        return new Declarables(
                exchange,
                retryExchange,
                dlqExchange,
                queue,
                retryQueue,
                dlqQueue,
                bindQueue,
                bindRetryQueue,
                bindDlqQueue
        );
    }
}
