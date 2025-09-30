package com.teuportal.core.bootstrap;

import jakarta.annotation.PostConstruct;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("singleCompanyBootstrapper")
@ConditionalOnProperty(value = "app.bootstrap.single-company.enabled", havingValue = "true", matchIfMissing = true)
public class SingleCompanyBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(SingleCompanyBootstrapper.class);

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final String defaultName;
    private final String defaultSlug;
    private final String adminEmail;

    public SingleCompanyBootstrapper(JdbcTemplate jdbcTemplate,
                                     Clock clock,
                                     @Value("${app.bootstrap.single-company.name:Local Company}") String defaultName,
                                     @Value("${app.bootstrap.single-company.slug:local}") String defaultSlug,
                                     @Value("${app.bootstrap.single-company.admin-email:}") String adminEmail) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.defaultName = defaultName;
        this.defaultSlug = defaultSlug;
        this.adminEmail = adminEmail == null ? "" : adminEmail.trim();
    }

    @PostConstruct
    public void ensureSingleCompanyExists() {
        UUID existing = fetchExistingCompanyId();
        if (existing != null) {
            log.info("Detected existing company {}", existing);
            return;
        }

        if (StringUtils.hasText(adminEmail)) {
            log.info("Bootstrapping single company with admin email '{}'", adminEmail);
        } else {
            log.info("Bootstrapping single company without admin email override");
        }

        UUID created = jdbcTemplate.execute((ConnectionCallback<UUID>) connection -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            UUID newId = UUID.randomUUID();
            try {
                applyTenantSession(connection, newId);
                insertCompany(connection, newId);
                insertSettings(connection, newId);
                seedAdminUser(connection, newId);
                connection.commit();
                return newId;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        });

        UUID resolved = created != null ? created : fetchExistingCompanyId();
        if (resolved == null) {
            throw new IllegalStateException("Failed to bootstrap single company row");
        }
        log.info("Bootstrapped single company '{}' ({})", defaultName, resolved);
    }

    private void applyTenantSession(Connection connection, UUID companyId) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET LOCAL app.company_id = '" + companyId + "'");
            statement.execute("SET LOCAL app.user_id = ''");
        }
    }

    private void insertCompany(Connection connection, UUID companyId) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO company (id, name, slug, is_active) VALUES (?, ?, ?, true)")) {
            insert.setObject(1, companyId);
            insert.setString(2, defaultName);
            insert.setString(3, defaultSlug);
            insert.executeUpdate();
        }
    }

    private void insertSettings(Connection connection, UUID companyId) throws SQLException {
        try (PreparedStatement settingsInsert = connection.prepareStatement(
                "INSERT INTO settings (company_id, configured, preferences) VALUES (?, false, '{}'::jsonb)")) {
            settingsInsert.setObject(1, companyId);
            settingsInsert.executeUpdate();
        }
    }

    private void seedAdminUser(Connection connection, UUID companyId) throws SQLException {
        String normalizedEmail = normalizeEmail(adminEmail);
        if (!StringUtils.hasText(normalizedEmail)) {
            return;
        }

        try (PreparedStatement check = connection.prepareStatement(
                "SELECT 1 FROM company_user WHERE company_id = ? AND lower(email) = lower(?) LIMIT 1")) {
            check.setObject(1, companyId);
            check.setString(2, normalizedEmail);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    log.info("Bootstrap admin user '{}' already exists for company {}", normalizedEmail, companyId);
                    return;
                }
            }
        }

        try (Statement disableRls = connection.createStatement()) {
            disableRls.execute("SET LOCAL row_security = off");
        }

        UUID userId = UUID.randomUUID();
        OffsetDateTime joinedAt = OffsetDateTime.now(clock);
        String displayName = deriveDisplayName(normalizedEmail);

        try (PreparedStatement insertUser = connection.prepareStatement(
                "INSERT INTO company_user (id, company_id, email, display_name, role, joined_at, last_sign_in_at) " +
                        "VALUES (?, ?, ?, ?, 'OWNER', ?, ?)")) {
            insertUser.setObject(1, userId);
            insertUser.setObject(2, companyId);
            insertUser.setString(3, normalizedEmail);
            insertUser.setString(4, displayName);
            insertUser.setObject(5, joinedAt);
            insertUser.setObject(6, joinedAt);
            insertUser.executeUpdate();
        }

        log.info("Bootstrapped OWNER user '{}' for company {}", normalizedEmail, companyId);
    }

    private UUID fetchExistingCompanyId() {
        return jdbcTemplate.query(
                "SELECT id FROM company WHERE is_active = true ORDER BY created_at ASC LIMIT 1",
                rs -> rs.next() ? rs.getObject(1, java.util.UUID.class) : null
        );
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String deriveDisplayName(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "New User";
        }
        String localPart = email.substring(0, email.indexOf('@'));
        String cleaned = localPart.replace('.', ' ').replace('_', ' ').replace('-', ' ').trim();
        if (!StringUtils.hasText(cleaned)) {
            cleaned = localPart;
        }
        String[] segments = cleaned.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(segment.charAt(0)));
            if (segment.length() > 1) {
                builder.append(segment.substring(1));
            }
        }
        String result = builder.toString().trim();
        return result.isEmpty() ? email : result;
    }
}