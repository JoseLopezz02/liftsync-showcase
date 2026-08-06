package com.liftsync.messaging.email;

import java.nio.charset.StandardCharsets;

import com.liftsync.config.i18n.MessageResolver;
import com.liftsync.exception.ServiceUnavailableException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Transport concern: turns a rendered subject + HTML body into an actual
 * outgoing message. Every email type shares the single sent path.
 */
@Component
public class EmailSender {

    private final JavaMailSender javaMailSender;
    private final MessageResolver messageResolver;
    private final String noreplyEmail;

    public EmailSender(
            JavaMailSender javaMailSender,
            MessageResolver messageResolver,
            @Value("${spring.mail.username}") String noreplyEmail
    ) {
        this.javaMailSender = javaMailSender;
        this.messageResolver = messageResolver;
        this.noreplyEmail = noreplyEmail;
    }

    public void sendHtml(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setTo(to);
            helper.setFrom(noreplyEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new ServiceUnavailableException(messageResolver.getMessage("error.email.delivery"), e);
        }
    }
}
