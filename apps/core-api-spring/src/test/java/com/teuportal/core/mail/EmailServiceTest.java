package com.teuportal.core.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmailServiceTest {

    // When email is disabled the sender is never invoked.
    @Test
    void sendDoesNothingWhenDisabled() {
        EmailProperties props = new EmailProperties();
        props.setEnabled(false);
        EmailSender sender = mock(EmailSender.class);
        EmailService service = new EmailService(sender, props);

        service.send(EmailMessage.text(List.of("dev@example.com"), "Subject", "Body"));

        verifyNoInteractions(sender);
    }

    // When enabled the service supplies the default from address before delegating.
    @Test
    void sendDelegatesWithDefaultFromFallback() {
        EmailProperties props = new EmailProperties();
        props.setEnabled(true);
        props.setDefaultFrom("noreply@example.com");
        props.getSmtp().setHost("localhost");

        EmailSender sender = mock(EmailSender.class);
        EmailService service = new EmailService(sender, props);

        service.send(EmailMessage.text(List.of("dev@example.com"), "Subject", "Body"));

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().from()).isEqualTo("noreply@example.com");
    }

    // Delivery exceptions raised by the sender flow back to the caller.
    @Test
    void sendPropagatesSenderFailure() {
        EmailProperties props = new EmailProperties();
        props.setEnabled(true);
        props.setDefaultFrom("noreply@example.com");
        props.getSmtp().setHost("localhost");
        EmailSender sender = mock(EmailSender.class);
        doThrow(new EmailDeliveryException("fail", new RuntimeException("boom"))).when(sender)
            .send(any(EmailMessage.class));

        EmailService service = new EmailService(sender, props);

        assertThatThrownBy(() -> service.send(EmailMessage.text(List.of("dev@example.com"), "Subject", "Body")))
            .isInstanceOf(EmailDeliveryException.class)
            .hasMessageContaining("fail");
    }
}
