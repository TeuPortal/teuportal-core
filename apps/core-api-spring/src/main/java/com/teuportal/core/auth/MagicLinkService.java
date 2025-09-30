package com.teuportal.core.auth;

import com.teuportal.core.company.CompanyUserService;
import com.teuportal.core.company.CompanyUserSummary;
import com.teuportal.core.mail.EmailMessage;
import com.teuportal.core.mail.EmailProperties;
import com.teuportal.core.mail.EmailService;
import com.teuportal.core.tenancy.TenantContextService;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class MagicLinkService {

    private static final Logger log = LoggerFactory.getLogger(MagicLinkService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EmailService emailService;
    private final EmailProperties emailProperties;
    private final TenantContextService tenantContextService;
    private final CompanyUserService companyUserService;
    private final Clock clock;
    private final Duration ttl;
    private final String appBaseUrl;

    public MagicLinkService(NamedParameterJdbcTemplate jdbcTemplate,
                            EmailService emailService,
                            EmailProperties emailProperties,
                            TenantContextService tenantContextService,
                            CompanyUserService companyUserService,
                            Clock clock,
                            @Value("${app.base-url}") String appBaseUrl,
                            @Value("${auth.magic-link.ttl-minutes:15}") long ttlMinutes) {
        this.jdbcTemplate = jdbcTemplate;
        this.emailService = emailService;
        this.emailProperties = emailProperties;
        this.tenantContextService = tenantContextService;
        this.companyUserService = companyUserService;
        this.clock = clock;
        this.ttl = Duration.ofMinutes(ttlMinutes);
        Assert.hasText(appBaseUrl, "app.base-url must be configured");
        this.appBaseUrl = appBaseUrl;
    }

    public void requestMagicLink(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (!StringUtils.hasText(normalizedEmail)) {
            log.debug("Skipping magic link request for empty email input");
            return;
        }

        UUID companyId = tenantContextService.currentCompanyId()
                .orElseThrow(() -> new IllegalStateException("Company context must be available"));
        Optional<CompanyUserSummary> user = companyUserService.findByEmail(companyId, normalizedEmail);
        if (user.isEmpty()) {
            log.info("Magic link requested for non-existent user '{}'; skipping issuance", normalizedEmail);
            return;
        }
        LoginToken token = createToken(normalizedEmail);
        String link = buildMagicLink(token.nonce());
        log.debug("Issued magic link token {} for {} expiring at {}", token.id(), normalizedEmail, token.expiresAt());

        emailService.send(EmailMessage.text(List.of(normalizedEmail),
                "Your teuportal sign-in link",
                createEmailBody(link)));

        if (!emailProperties.isEnabled()) {
            log.info("Email disabled - magic link for {} => {}", normalizedEmail, link);
        }
    }

    public String consumeMagicLink(String tokenValue) {
        String nonce = tokenValue == null ? "" : tokenValue.trim();
        if (nonce.isEmpty()) {
            throw new InvalidMagicLinkException("Missing magic link token");
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("nonce", nonce)
                .addValue("usedAt", now)
                .addValue("now", now);

        return jdbcTemplate.query("""
                UPDATE login_token
                SET used_at = :usedAt
                WHERE nonce = :nonce
                  AND used_at IS NULL
                  AND expires_at >= :now
                RETURNING email
                """, params, (rs, rowNum) -> rs.getString("email"))
                .stream()
                .findFirst()
                .orElseThrow(() -> new InvalidMagicLinkException("Magic link is invalid or expired"));
    }

    private LoginToken createToken(String email) {
        UUID id = UUID.randomUUID();
        String nonce = generateNonce();
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime expiresAt = now.plus(ttl);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("email", email)
                .addValue("nonce", nonce)
                .addValue("createdAt", now)
                .addValue("expiresAt", expiresAt);
        jdbcTemplate.update("""
                INSERT INTO login_token (id, email, nonce, created_at, expires_at)
                VALUES (:id, :email, :nonce, :createdAt, :expiresAt)
                """, params);
        return new LoginToken(id, email, nonce, expiresAt);
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String buildMagicLink(String nonce) {
        return UriComponentsBuilder.fromUriString(appBaseUrl)
                .path("/api/auth/magic")
                .queryParam("token", nonce)
                .build()
                .toUriString();
    }

    private static String createEmailBody(String link) {
        return "Click the link below to sign in to teuportal. The link expires soon." +
                "\n\n" + link + "\n";
    }

    private static String generateNonce() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record LoginToken(UUID id, String email, String nonce, OffsetDateTime expiresAt) {
    }

    public static class InvalidMagicLinkException extends RuntimeException {
        public InvalidMagicLinkException(String message) {
            super(message);
        }
    }
}