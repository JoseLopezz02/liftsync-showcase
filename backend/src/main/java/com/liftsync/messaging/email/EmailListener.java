package com.liftsync.messaging.email;

import com.liftsync.config.rabbitmq.properties.EmailMessagingProperties;
import com.liftsync.exception.EmailMessageValidationException;
import com.liftsync.service.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EmailListener {

    private static final Logger logger = LoggerFactory.getLogger(EmailListener.class);

    private final EmailMessageDispatcher emailMessageDispatcher;
    private final EmailPublisher emailPublisher;
    private final EmailMessagingProperties emailMessagingProperties;
    private final EmailService emailService;

    public EmailListener(
            EmailMessageDispatcher emailMessageDispatcher,
            EmailPublisher emailPublisher,
            EmailMessagingProperties emailMessagingProperties,
            EmailService emailService
    ) {
        this.emailMessageDispatcher = emailMessageDispatcher;
        this.emailPublisher = emailPublisher;
        this.emailMessagingProperties = emailMessagingProperties;
        this.emailService = emailService;
    }

    @RabbitListener(queues = "${app.messaging.email.queue}")
    public void receiveEmailMessage(EmailMessage emailMessage, Message message) {
        try {

            emailService.validateEmailMessageToPublish(emailMessage);
            emailMessageDispatcher.dispatch(emailMessage);

        } catch (EmailMessageValidationException ex) {

            logger.error("Rejecting invalid email message type={} to={} reason={}",
                    emailMessage != null ? emailMessage.type() : null,
                    emailMessage != null ? emailMessage.to() : null,
                    ex.getMessage());

            if (emailMessage != null) {
                emailPublisher.publishToDeadLetter(emailMessage, ex.getMessage());
            }

            throw new AmqpRejectAndDontRequeueException(ex.getMessage(), ex);

        } catch (Exception ex) {
            handleProcessingFailure(emailMessage, message, ex);
            throw new AmqpRejectAndDontRequeueException(ex.getMessage(), ex);
        }
    }

    private void handleProcessingFailure(EmailMessage emailMessage, Message message, Exception ex) {
        String failureReason = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        int currentRetryCount = getRetryCount(message);
        int nextRetryCount = currentRetryCount + 1;

        logger.warn(
                "Email processing failed type={} to={} retry={}/{} reason={}",
                emailMessage != null ? emailMessage.type() : null,
                emailMessage != null ? emailMessage.to() : null,
                nextRetryCount,
                emailMessagingProperties.maxRetries(),
                failureReason,
                ex
        );

        if (emailMessage == null) {
            return;
        }

        if (nextRetryCount <= emailMessagingProperties.maxRetries()) {
            emailPublisher.publishToRetry(emailMessage, nextRetryCount, failureReason);
            return;
        }

        emailPublisher.publishToDeadLetter(emailMessage, failureReason);
    }

    private int getRetryCount(Message message) {
        if (message == null) {
            return 0;
        }

        Object retryCount = message.getMessageProperties()
                .getHeaders()
                .get(emailMessagingProperties.retryCountHeader());

        if (retryCount instanceof Number number) {
            return number.intValue();
        }

        return 0;
    }
}
