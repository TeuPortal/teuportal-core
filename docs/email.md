# Email Delivery

Teuportal Core ships with a simple email service that can send messages over SMTP today and leaves room for plugging in other providers later.

## Service
- `EmailService` lives under `com.teuportal.core.mail` and accepts an `EmailMessage` describing recipients, subject, and optional text/HTML bodies.
- When `app.email.enabled=false` the service is effectively a no-op so API flows stay side-effect free in local/dev setups.
- The active transport is resolved once at startup via `EmailConfiguration`; additional providers can be registered by adding implementations of `EmailSender` and expanding the enum on `EmailProperties`.

### Example
```java
@Autowired
private EmailService emailService;

public void sendInvite(String to, String link) {
    EmailMessage message = EmailMessage.html(
        List.of(to),
        "You are invited",
        "<p>Click <a href='" + link + "'>here</a> to finish sign-in.</p>"
    );
    emailService.send(message);
}
```

## Configuration
Email settings reside under the `app.email.*` namespace. Values can be provided through `config/api-local.properties` for developers or environment variables in deployed environments.

| Property | Env var | Default | Notes |
| --- | --- | --- | --- |
| `app.email.enabled` | `APP_EMAIL_ENABLED` | `false` | Master switch; keep `false` when working offline.
| `app.email.provider` | `APP_EMAIL_PROVIDER` | `smtp` | Only `smtp` is implemented today; reserved for future providers.
| `app.email.default-from` | `APP_EMAIL_DEFAULT_FROM` | _(empty)_ | Required when email is enabled.
| `app.email.smtp.host` | `APP_EMAIL_SMTP_HOST` | _(empty)_ | Hostname of the SMTP relay.
| `app.email.smtp.port` | `APP_EMAIL_SMTP_PORT` | `587` | Port for the SMTP relay.
| `app.email.smtp.username` | `APP_EMAIL_SMTP_USERNAME` | _(empty)_ | Optional when auth is disabled.
| `app.email.smtp.password` | `APP_EMAIL_SMTP_PASSWORD` | _(empty)_ | Optional when auth is disabled.
| `app.email.smtp.auth` | `APP_EMAIL_SMTP_AUTH` | `false` | Toggle SMTP AUTH; requires username/password when `true`.
| `app.email.smtp.start-tls` | `APP_EMAIL_SMTP_STARTTLS` | `true` | Enables STARTTLS for secure connections.

Local developers can point the service at MailHog/Mailpit by enabling email and setting host/port to the container.

## Behaviour
- SMTP configuration is validated during startup; missing host/default sender (or credentials when auth is on) fails fast rather than at send time.
- Delivery failures throw `EmailDeliveryException`; callers should decide whether to retry or surface the issue.
- When disabled, the service logs at debug level and returns immediately without throwing.
