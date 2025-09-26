package com.teuportal.core.mail;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.Assert;

@Configuration
@EnableConfigurationProperties(EmailProperties.class)
public class EmailConfiguration {

    @Bean
    public EmailSender emailSender(EmailProperties properties) {
        if (!properties.isEnabled()) {
            return new NoopEmailSender();
        }

        return switch (properties.getProvider()) {
            case SMTP -> createSmtpSender(properties);
        };
    }

    private EmailSender createSmtpSender(EmailProperties properties) {
        EmailProperties.Smtp smtp = properties.getSmtp();
        Assert.hasText(properties.getDefaultFrom(), "app.email.default-from must be set when email is enabled");
        Assert.hasText(smtp.getHost(), "app.email.smtp.host must be set when email is enabled");

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtp.getHost());
        sender.setPort(smtp.getPort());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        if (smtp.getUsername() != null && !smtp.getUsername().isBlank()) {
            sender.setUsername(smtp.getUsername());
            sender.setPassword(smtp.getPassword());
        }

        if (smtp.isAuth()) {
            Assert.hasText(sender.getUsername(), "app.email.smtp.username must be set when SMTP auth is enabled");
        }

        Properties javaMailProps = sender.getJavaMailProperties();
        javaMailProps.put("mail.transport.protocol", "smtp");
        javaMailProps.put("mail.smtp.auth", String.valueOf(smtp.isAuth()));
        javaMailProps.put("mail.smtp.starttls.enable", String.valueOf(smtp.isStartTls()));
        javaMailProps.put("mail.smtp.starttls.required", String.valueOf(smtp.isStartTls()));
        javaMailProps.put("mail.smtp.connectiontimeout", "5000");
        javaMailProps.put("mail.smtp.timeout", "5000");
        javaMailProps.put("mail.smtp.writetimeout", "5000");

        return new SmtpEmailSender(sender);
    }
}
