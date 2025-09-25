package com.teuportal.core.bootstrap;

import jakarta.annotation.PostConstruct;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("singleCompanyBootstrapper")
@ConditionalOnProperty(value = "app.bootstrap.single-company.enabled", havingValue = "true", matchIfMissing = true)
public class SingleCompanyBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(SingleCompanyBootstrapper.class);

    private final JdbcTemplate jdbcTemplate;
    private final String defaultName;
    private final String defaultSlug;

    public SingleCompanyBootstrapper(JdbcTemplate jdbcTemplate,
                                     @Value("${app.bootstrap.single-company.name:Local Company}") String defaultName,
                                     @Value("${app.bootstrap.single-company.slug:local}") String defaultSlug) {
        this.jdbcTemplate = jdbcTemplate;
        this.defaultName = defaultName;
        this.defaultSlug = defaultSlug;
    }

    @PostConstruct
    public void ensureSingleCompanyExists() {
        UUID existing = fetchExistingCompanyId();
        if (existing != null) {
            log.info("Detected existing company {}", existing);
            return;
        }

        UUID created = jdbcTemplate.execute((ConnectionCallback<UUID>) connection -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            UUID newId = UUID.randomUUID();
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET LOCAL app.company_id = '" + newId + "'");
                    statement.execute("SET LOCAL app.user_id = ''");
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO company (id, name, slug, is_active) VALUES (?, ?, ?, true)")) {
                    insert.setObject(1, newId);
                    insert.setString(2, defaultName);
                    insert.setString(3, defaultSlug);
                    insert.executeUpdate();
                }
                try (PreparedStatement settingsInsert = connection.prepareStatement(
                        "INSERT INTO settings (company_id, configured, preferences) VALUES (?, false, '{}'::jsonb)")) {
                    settingsInsert.setObject(1, newId);
                    settingsInsert.executeUpdate();
                }
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

    private UUID fetchExistingCompanyId() {
        return jdbcTemplate.query(
                "SELECT id FROM company WHERE is_active = true ORDER BY created_at ASC LIMIT 1",
                rs -> rs.next() ? rs.getObject(1, java.util.UUID.class) : null
        );
    }
}
