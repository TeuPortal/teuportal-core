package com.teuportal.core.auth;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ExternalAccountRepository {

    private static final RowMapper<ExternalAccountRecord> ROW_MAPPER = (rs, rowNum) -> new ExternalAccountRecord(
            rs.getObject("id", java.util.UUID.class),
            rs.getObject("company_id", java.util.UUID.class),
            rs.getObject("user_id", java.util.UUID.class),
            rs.getString("provider"),
            rs.getString("provider_user_id"),
            rs.getString("email"),
            rs.getObject("created_at", java.time.OffsetDateTime.class)
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ExternalAccountRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ExternalAccountRecord> findByProviderAndSubject(String provider, String providerUserId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("provider", provider)
                .addValue("providerUserId", providerUserId);
        List<ExternalAccountRecord> results = jdbcTemplate.query("""
                SELECT id, company_id, user_id, provider, provider_user_id, email, created_at
                FROM external_account
                WHERE provider = :provider AND provider_user_id = :providerUserId
                LIMIT 1
                """, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    public ExternalAccountRecord upsert(UUID companyId,
                                        UUID userId,
                                        String provider,
                                        String providerUserId,
                                        String email,
                                        OffsetDateTime now) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("companyId", companyId)
                .addValue("userId", userId)
                .addValue("provider", provider)
                .addValue("providerUserId", providerUserId)
                .addValue("email", email)
                .addValue("createdAt", now);
        List<ExternalAccountRecord> results = jdbcTemplate.query("""
                INSERT INTO external_account (id, company_id, user_id, provider, provider_user_id, email, created_at)
                VALUES (:id, :companyId, :userId, :provider, :providerUserId, :email, :createdAt)
                ON CONFLICT (provider, provider_user_id) DO UPDATE
                SET user_id = EXCLUDED.user_id,
                    email = EXCLUDED.email
                RETURNING id, company_id, user_id, provider, provider_user_id, email, created_at
                """, params, ROW_MAPPER);
        if (results.isEmpty()) {
            throw new IllegalStateException("Failed to upsert external account");
        }
        return results.getFirst();
    }

    public record ExternalAccountRecord(
            UUID id,
            UUID companyId,
            UUID userId,
            String provider,
            String providerUserId,
            String email,
            OffsetDateTime createdAt
    ) {
    }
}

