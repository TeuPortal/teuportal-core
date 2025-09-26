package com.teuportal.core.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);
    private static final String TAG_REGEX = "<[^>]+>";

    private final JavaMailSender delegate;

    public SmtpEmailSender(JavaMailSender delegate) {
        this.delegate = delegate;
    }

    @Override
    public void send(EmailMessage message) {
        try {
            MimeMessage mimeMessage = delegate.createMimeMessage();
            boolean hasHtml = message.htmlBody() != null && !message.htmlBody().isBlank();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, hasHtml, StandardCharsets.UTF_8.name());

            helper.setTo(message.to().toArray(String[]::new));
            if (!message.cc().isEmpty()) {
                helper.setCc(message.cc().toArray(String[]::new));
            }
            if (!message.bcc().isEmpty()) {
                helper.setBcc(message.bcc().toArray(String[]::new));
            }

            if (message.from() != null && !message.from().isBlank()) {
                helper.setFrom(message.from());
            }

            helper.setSubject(message.subject());

            if (hasHtml) {
                String plain = message.textBody();
                if (plain == null || plain.isBlank()) {
                    plain = stripHtml(message.htmlBody());
                }
                helper.setText(plain, message.htmlBody());
            } else {
                helper.setText(message.textBody() == null ? "" : message.textBody(), false);
            }

            delegate.send(mimeMessage);
            log.debug("Email sent via SMTP to {}", message.to());
        } catch (MailException | MessagingException ex) {
            throw new EmailDeliveryException("Failed to send email message", ex);
        }
    }

    private String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll(TAG_REGEX, "").replaceAll("\\s+", " ").trim();
    }
}
