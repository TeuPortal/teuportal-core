package com.teuportal.core.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpEmailSenderTest {

    private JavaMailSender mailSender;
    private SmtpEmailSender sender;

    @BeforeEach
    void setUp() {
        mailSender = Mockito.mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        Mockito.when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        sender = new SmtpEmailSender(mailSender);
    }

    // Sends a plain text email and verifies subject, recipient, and content type.
    @Test
    void sendsTextMessage() throws Exception {
        EmailMessage message = EmailMessage.text(List.of("a@example.com"), "Hi", "Hello");

        sender.send(message);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        MimeMessage built = captor.getValue();
        assertThat(built.getSubject()).isEqualTo("Hi");
        assertThat(built.getAllRecipients()[0].toString()).isEqualTo("a@example.com");
        assertThat(built.getContentType()).contains("text/plain");
    }

    // Sends an HTML email and checks the MIME payload includes both plain and HTML parts.
    @Test
    void sendsHtmlMessage() throws Exception {
        EmailMessage message = EmailMessage.html(List.of("html@example.com"), "Hi", "<strong>Hello</strong>");

        sender.send(message);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        Mockito.verify(mailSender).send(captor.capture());
        MimeMessage built = captor.getValue();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        built.writeTo(baos);
        String raw = baos.toString(StandardCharsets.UTF_8);

        assertThat(raw).contains("multipart");
        assertThat(raw).contains("text/plain");
        assertThat(raw).contains("text/html");
        assertThat(raw).contains("<strong>Hello</strong>");
    }

    // Confirms JavaMailSender failures surface as EmailDeliveryException.
    @Test
    void wrapsMailExceptions() {
        JavaMailSender failingSender = Mockito.mock(JavaMailSender.class);
        Mockito.when(failingSender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));
        Mockito.doThrow(new MailSendException("fail")).when(failingSender).send(Mockito.any(MimeMessage.class));
        SmtpEmailSender failing = new SmtpEmailSender(failingSender);

        assertThatThrownBy(() -> failing.send(EmailMessage.text(List.of("a@example.com"), "Subject", "Body")))
            .isInstanceOf(EmailDeliveryException.class)
            .hasMessageContaining("Failed to send email message");
    }
}
