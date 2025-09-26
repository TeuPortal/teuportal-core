package com.teuportal.core.mail;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record EmailMessage(
        List<String> to,
        List<String> cc,
        List<String> bcc,
        String subject,
        String textBody,
        String htmlBody,
        String from
) {
    public EmailMessage {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(subject, "subject");

        if (to.isEmpty()) {
            throw new IllegalArgumentException("At least one recipient is required");
        }

        to = List.copyOf(to);
        cc = cc == null ? List.of() : List.copyOf(cc);
        bcc = bcc == null ? List.of() : List.copyOf(bcc);
    }

    public static EmailMessage text(Collection<String> to, String subject, String body) {
        return new EmailMessage(List.copyOf(to), List.of(), List.of(), subject, body, null, null);
    }

    public static EmailMessage html(Collection<String> to, String subject, String body) {
        return new EmailMessage(List.copyOf(to), List.of(), List.of(), subject, null, body, null);
    }

    public EmailMessage withFromFallback(String fallbackFrom) {
        if (from != null && !from.isBlank()) {
            return this;
        }
        return new EmailMessage(to, cc, bcc, subject, textBody, htmlBody, fallbackFrom);
    }
}
