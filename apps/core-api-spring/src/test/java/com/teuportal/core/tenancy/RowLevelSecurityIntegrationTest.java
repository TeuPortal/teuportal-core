package com.teuportal.core.tenancy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.postgresql.util.PSQLException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Testcontainers
class RowLevelSecurityIntegrationTest {

    private static final String APP_USER = "app_user";
    private static final String APP_PASSWORD = "app_password";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("teuportal")
            .withUsername("postgres")
            .withPassword("postgres");

    private DataSource dataSource;

    @BeforeAll
    static void configureDatabaseRole() throws SQLException {
        POSTGRES.start();
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto");
            statement.execute("DO $$\nBEGIN\n    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '" + APP_USER + "') THEN\n        CREATE ROLE " + APP_USER + " LOGIN PASSWORD '" + APP_PASSWORD + "';\n    END IF;\nEND;\n$$;");
        }
    }

    @BeforeEach
    void resetSchema() throws SQLException {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas("public", "app")
                .cleanDisabled(false)
                .locations("classpath:db/migration")
                .load();
        flyway.clean();
        flyway.migrate();
        grantApplicationPrivileges();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(POSTGRES.getJdbcUrl());
        config.setUsername(APP_USER);
        config.setPassword(APP_PASSWORD);
        config.setMaximumPoolSize(4);
        dataSource = new HikariDataSource(config);
    }

    @AfterEach
    void tearDown() {
        if (dataSource instanceof HikariDataSource hikari) {
            hikari.close();
        }
    }

    @Test
    void shouldRestrictReadsToCurrentTenantContext() throws Exception {
        CompanyContext companyA = insertCompanyWithContext("Company A", "company-a");
        CompanyContext companyB = insertCompanyWithContext("Company B", "company-b");

        UUID clientA = insertClient(companyA, companyA.companyId(), "Client A");
        insertClient(companyB, companyB.companyId(), "Client B");

        List<String> clientsForA = selectClientNames(companyA);
        List<String> clientsForB = selectClientNames(companyB);
        Assertions.assertEquals(List.of("Client A"), clientsForA, "tenant A should only see its rows");
        Assertions.assertEquals(List.of("Client B"), clientsForB, "tenant B should only see its rows");
    }

    @Test
    void shouldRejectCrossTenantWrites() throws Exception {
        CompanyContext companyA = insertCompanyWithContext("Company A", "company-a");
        CompanyContext companyB = insertCompanyWithContext("Company B", "company-b");

        insertClient(companyA, companyA.companyId(), "Client A");
        insertClient(companyB, companyB.companyId(), "Client B");

        List<String> expectedClientsForB = selectClientNames(companyB);

        Assertions.assertTrue(executeExpectingPsqlException(() -> insertClient(companyA, companyB.companyId(), "Intruder")),
                "cross-tenant insert should raise a PSQLException");
        Assertions.assertEquals(expectedClientsForB, selectClientNames(companyB),
                "cross-tenant insert should not mutate target tenant data");

        executeExpectingPsqlException(() -> updateClientName(companyA, companyB.companyId(), "Client B", "Hacked"));
        Assertions.assertEquals(expectedClientsForB, selectClientNames(companyB),
                "cross-tenant update should leave target tenant data unchanged");
    }

    @Test
    void shouldRejectCrossTenantDeletes() throws Exception {
        CompanyContext companyA = insertCompanyWithContext("Company A", "company-a");
        CompanyContext companyB = insertCompanyWithContext("Company B", "company-b");

        UUID clientB = insertClient(companyB, companyB.companyId(), "Client B");
        List<String> expectedClientsForB = selectClientNames(companyB);

        executeExpectingPsqlException(() -> deleteClient(companyA, clientB));
        Assertions.assertEquals(expectedClientsForB, selectClientNames(companyB),
                "cross-tenant delete should leave target tenant data unchanged");
    }

    @Test
    void shouldRejectQueriesWithoutContext() throws Exception {
        CompanyContext company = insertCompanyWithContext("Company", "company");
        insertClient(company, company.companyId(), "Client");

        Assertions.assertThrows(PSQLException.class, () -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement("SELECT name FROM client")) {
                ps.executeQuery();
            }
        });
    }

    private CompanyContext insertCompanyWithContext(String name, String slug) throws SQLException {
        UUID companyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET LOCAL app.company_id = '" + companyId + "'");
                statement.execute("SET LOCAL app.user_id = '" + ownerId + "'");
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO company (id, name, slug, is_active) VALUES (?, ?, ?, true)")) {
                ps.setObject(1, companyId);
                ps.setString(2, name);
                ps.setString(3, slug);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO settings (company_id, configured, preferences) VALUES (?, false, '{}'::jsonb)")) {
                ps.setObject(1, companyId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO company_user (id, company_id, email, display_name, role, created_at, updated_at) VALUES (?, ?, ?, ?, 'OWNER', now(), now())")) {
                ps.setObject(1, ownerId);
                ps.setObject(2, companyId);
                ps.setString(3, slug + "@example.com");
                ps.setString(4, name + " Owner");
                ps.executeUpdate();
            }
            connection.commit();
        }
        return new CompanyContext(companyId, ownerId);
    }

    private UUID insertClient(CompanyContext context, UUID targetCompanyId, String name) throws SQLException {
        UUID clientId = UUID.randomUUID();
        withCompanyContext(context.companyId(), context.ownerId(), connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO client (id, company_id, name, created_at, updated_at) VALUES (?, ?, ?, now(), now())")) {
                ps.setObject(1, clientId);
                ps.setObject(2, targetCompanyId);
                ps.setString(3, name);
                ps.executeUpdate();
            }
        });
        return clientId;
    }

    private void updateClientName(CompanyContext context, UUID targetCompanyId, String targetName, String newName) throws SQLException {
        withCompanyContext(context.companyId(), context.ownerId(), connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE client SET name = ?, updated_at = now() WHERE company_id = ? AND name = ?")) {
                ps.setString(1, newName);
                ps.setObject(2, targetCompanyId);
                ps.setString(3, targetName);
                ps.executeUpdate();
            }
        });
    }

    private void deleteClient(CompanyContext context, UUID clientId) throws SQLException {
        withCompanyContext(context.companyId(), context.ownerId(), connection -> {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM client WHERE id = ?")) {
                ps.setObject(1, clientId);
                ps.executeUpdate();
            }
        });
    }

    private List<String> selectClientNames(CompanyContext context) throws SQLException {
        List<String> results = new ArrayList<>();
        withCompanyContext(context.companyId(), context.ownerId(), connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM client ORDER BY name")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        results.add(rs.getString(1));
                    }
                }
            }
        });
        return results;
    }

    private boolean executeExpectingPsqlException(SqlRunnable operation) throws SQLException {
        try {
            operation.run();
            return false;
        } catch (PSQLException ex) {
            return true;
        }
    }


    private void withCompanyContext(UUID companyId, UUID userId, SqlConsumer consumer) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET LOCAL app.company_id = '" + companyId + "'");
                if (userId != null) {
                    statement.execute("SET LOCAL app.user_id = '" + userId + "'");
                } else {
                    statement.execute("SET LOCAL app.user_id = ''");
                }
            }
            try {
                consumer.accept(connection);
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }

    private void grantApplicationPrivileges() throws SQLException {
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("GRANT USAGE ON SCHEMA public TO " + APP_USER);
            statement.execute("GRANT USAGE ON SCHEMA app TO " + APP_USER);
            statement.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO " + APP_USER);
            statement.execute("GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA app TO " + APP_USER);
            statement.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO " + APP_USER);
            statement.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO " + APP_USER);
            statement.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO " + APP_USER);
            statement.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA app GRANT EXECUTE ON FUNCTIONS TO " + APP_USER);
        }
    }

    private record CompanyContext(UUID companyId, UUID ownerId) {}

    @FunctionalInterface
    interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    interface SqlRunnable {
        void run() throws SQLException;
    }
}
