package com.liftsync.messaging.email;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.liftsync.config.i18n.MessageResolver;
import com.liftsync.exception.InternalServerException;
import com.liftsync.messaging.email.handler.EmailMessageHandler;
import com.liftsync.service.locale.LocaleService;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


@Service
public class EmailMessageDispatcher {

    private final Map<EmailType, EmailMessageHandler> overridesByType;
    private final TemplateEngine templateEngine;
    private final EmailSender emailSender;
    private final MessageResolver messageResolver;
    private final LocaleService localeService;

    public EmailMessageDispatcher(
            List<EmailMessageHandler> overrides,
            TemplateEngine templateEngine,
            EmailSender emailSender,
            MessageResolver messageResolver,
            LocaleService localeService
    ) {
        this.overridesByType = new EnumMap<>(EmailType.class);
        this.templateEngine = templateEngine;
        this.emailSender = emailSender;
        this.messageResolver = messageResolver;
        this.localeService = localeService;

        for (EmailMessageHandler handler : overrides) {

            EmailMessageHandler previousHandler = overridesByType.putIfAbsent(handler.getSupportedEmailType(), handler);
            if (previousHandler != null) {
                throw new InternalServerException(
                        messageResolver.getMessage("error.email.handler.duplicate.type", handler.getSupportedEmailType())
                );
            }

        }
    }

    public void dispatch(EmailMessage emailMessage) {
        Locale previousLocale = localeService.getCurrentLocale();
        localeService.setLocale(emailMessage.locale());

        try {
            EmailMessageHandler override = overridesByType.get(emailMessage.type());
            if (override != null) {
                override.handle(emailMessage);
            } else {
                render(emailMessage);
            }
        } finally {
            localeService.setLocale(previousLocale);
        }
    }

    private void render(EmailMessage emailMessage) {
        EmailType type = emailMessage.type();

        Context context = new Context(localeService.getCurrentLocale());
        emailMessage.variables().forEach(context::setVariable);

        String body = templateEngine.process(type.getTemplateName(), context);
        String subject = resolveSubject(type, emailMessage.variables());

        emailSender.sendHtml(emailMessage.to(), subject, body);
    }

    private String resolveSubject(EmailType type, Map<String, String> variables) {
        String subjectArg = type.getSubjectArg();
        if (subjectArg == null) {
            return messageResolver.getMessage(type.getSubjectKey());
        }

        return messageResolver.getMessage(type.getSubjectKey(), variables.get(subjectArg));
    }

}
