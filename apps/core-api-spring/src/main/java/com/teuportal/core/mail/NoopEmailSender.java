package com.teuportal.core.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoopEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(NoopEmailSender.class);

    @Override
    public void send(EmailMessage message) {
        log.info("Email delivery disabled. Skipping send to {} with subject '{}'", message.to(), message.subject());
    }
}
