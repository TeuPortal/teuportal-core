package com.teuportal.core.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class EmailPropertiesTest {

    // Confirms the defaults are safe when nothing is configured.
    @Test
    void defaultsMatchExpectations() {
        EmailProperties props = new EmailProperties();

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getProvider()).isEqualTo(EmailProperties.Provider.SMTP);
        assertThat(props.getDefaultFrom()).isEmpty();
        assertThat(props.getSmtp().getPort()).isEqualTo(587);
        assertThat(props.getSmtp().isAuth()).isFalse();
        assertThat(props.getSmtp().isStartTls()).isTrue();
    }

    // Verifies relaxed binding maps env-style properties into the object graph.
    @Test
    void bindsFromConfigurationSource() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
            "app.email.enabled", "true",
            "app.email.default-from", "noreply@example.com",
            "app.email.smtp.host", "localhost",
            "app.email.smtp.port", "2525",
            "app.email.smtp.auth", "true",
            "app.email.smtp.username", "user",
            "app.email.smtp.password", "secret",
            "app.email.smtp.start-tls", "false"
        ));

        Binder binder = new Binder(source);
        EmailProperties props = binder.bind("app.email", Bindable.of(EmailProperties.class))
            .orElseThrow(() -> new IllegalStateException("binding failed"));

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getDefaultFrom()).isEqualTo("noreply@example.com");
        assertThat(props.getSmtp().getHost()).isEqualTo("localhost");
        assertThat(props.getSmtp().getPort()).isEqualTo(2525);
        assertThat(props.getSmtp().isAuth()).isTrue();
        assertThat(props.getSmtp().getUsername()).isEqualTo("user");
        assertThat(props.getSmtp().getPassword()).isEqualTo("secret");
        assertThat(props.getSmtp().isStartTls()).isFalse();
    }
}
