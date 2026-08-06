package com.liftsync.messaging.email;

import com.liftsync.config.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.liftsync.config.rabbitmq.properties.EmailMessagingProperties;
import com.liftsync.config.i18n.MessageResolver;
import com.liftsync.exception.ServiceUnavailableException;

@Component
public class EmailPublisher {

    private static final Logger logger = LoggerFactory.getLogger(EmailPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MessageResolver messageResolver;
    private final EmailMessagingProperties emailMessagingProperties;
    private final TransactionManager transactionManager;

    public EmailPublisher(
            RabbitTemplate rabbitTemplate,
            MessageResolver messageResolver,
            EmailMessagingProperties emailMessagingProperties,
            TransactionManager transactionManager
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.messageResolver = messageResolver;
        this.emailMessagingProperties = emailMessagingProperties;
        this.transactionManager = transactionManager;
    }

    public void publish(EmailMessage emailMessage) {
        transactionManager.runAfterCommit(() ->
                publish(
                    emailMessage,
                    emailMessagingProperties.exchange(),
                    emailMessagingProperties.routingKey(),
                    null,
                    null
                )
        );

        logger.info("Queued email message type={} to={}", emailMessage.type(), emailMessage.to());
    }

    public void publishToRetry(EmailMessage emailMessage, int retryCount, String failureReason) {
        publish(
                emailMessage,
                emailMessagingProperties.retryExchange(),
                emailMessagingProperties.retryRoutingKey(),
                retryCount,
                failureReason
        );

        logger.warn(
                "Retrying email message type={} to={} attempt={}/{}",
                emailMessage.type(),
                emailMessage.to(),
                retryCount,
                emailMessagingProperties.maxRetries()
        );
    }

    public void publishToDeadLetter(EmailMessage emailMessage, String failureReason) {
        publish(
                emailMessage,
                emailMessagingProperties.dlqExchange(),
                emailMessagingProperties.dlqRoutingKey(),
                null,
                failureReason
        );

        logger.error("Dead-lettered email message type={} to={} reason={}", emailMessage.type(), emailMessage.to(), failureReason);
    }

    private void publish(
            EmailMessage emailMessage,
            String exchange,
            String routingKey,
            Integer retryCount,
            String failureReason
    ) {
        try {

            rabbitTemplate.convertAndSend(
                    exchange,
                    routingKey,
                    emailMessage,
                    message -> {

                        if (retryCount != null) {
                            message.getMessageProperties()
                                    .setHeader(emailMessagingProperties.retryCountHeader(), retryCount);
                        }

                        if (failureReason != null && !failureReason.isBlank()) {
                            message.getMessageProperties()
                                    .setHeader(emailMessagingProperties.failureReasonHeader(), failureReason);
                        }

                        return message;
                    }
            );

        } catch (AmqpException ex) {
            String message = messageResolver.getMessage("error.email.queue");
            throw new ServiceUnavailableException(message, ex);
        }
    }
}
