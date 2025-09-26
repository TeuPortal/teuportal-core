package com.teuportal.core.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final EmailSender emailSender;
    private final EmailProperties properties;

    public EmailService(EmailSender emailSender, EmailProperties properties) {
        this.emailSender = emailSender;
        this.properties = properties;
    }

    public void send(EmailMessage message) {
        Assert.notNull(message, "message must not be null");

        if (!properties.isEnabled()) {
            log.debug("Email disabled. Dropping email to {}", message.to());
            return;
        }

        EmailMessage resolved = message.withFromFallback(properties.getDefaultFrom());
        emailSender.send(resolved);
    }
}
