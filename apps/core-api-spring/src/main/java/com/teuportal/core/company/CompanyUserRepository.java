package com.teuportal.core.company;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyUserRepository {

    private static final RowMapper<CompanyUserSummary> ROW_MAPPER = (rs, rowNum) -> new CompanyUserSummary(
            rs.getObject("id", java.util.UUID.class),
            rs.getString("email"),
            rs.getString("display_name"),
            rs.getString("role"),
            rs.getObject("joined_at", java.time.OffsetDateTime.class)
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CompanyUserRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CompanyUserSummary> findRecentMembers(int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("limit", limit);
        return jdbcTemplate.query("""
                SELECT id, email, display_name, role, joined_at
                FROM company_user
                ORDER BY joined_at DESC NULLS LAST, created_at DESC
                LIMIT :limit
                """, params, ROW_MAPPER);
    }

    public boolean existsById(UUID id) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM company_user WHERE id = :id", params, Integer.class);
        return count != null && count > 0;
    }

    public Optional<CompanyUserSummary> findByEmail(UUID companyId, String email) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("email", email);
        List<CompanyUserSummary> results = jdbcTemplate.query("""
                SELECT id, email, display_name, role, joined_at
                FROM company_user
                WHERE company_id = :companyId AND lower(email) = lower(:email)
                LIMIT 1
                """, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    public Optional<CompanyUserSummary> findById(UUID companyId, UUID userId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("companyId", companyId)
                .addValue("userId", userId);
        List<CompanyUserSummary> results = jdbcTemplate.query("""
                SELECT id, email, display_name, role, joined_at
                FROM company_user
                WHERE company_id = :companyId AND id = :userId
                LIMIT 1
                """, params, ROW_MAPPER);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }
    public CompanyUserSummary create(UUID companyId,
                                     UUID userId,
                                     String email,
                                     String displayName,
                                     String role,
                                     OffsetDateTime joinedAt) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", userId)
                .addValue("companyId", companyId)
                .addValue("email", email)
                .addValue("displayName", displayName)
                .addValue("role", role)
                .addValue("joinedAt", joinedAt)
                .addValue("lastSignInAt", joinedAt);
        List<CompanyUserSummary> results = jdbcTemplate.query("""
                INSERT INTO company_user (id, company_id, email, display_name, role, joined_at, last_sign_in_at)
                VALUES (:id, :companyId, :email, :displayName, :role, :joinedAt, :lastSignInAt)
                RETURNING id, email, display_name, role, joined_at
                """, params, ROW_MAPPER);
        if (results.isEmpty()) {
            throw new IllegalStateException("Failed to create company user record");
        }
        return results.getFirst();
    }

    public boolean existsAnyForCompany(UUID companyId) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("companyId", companyId);
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM company_user WHERE company_id = :companyId", params, Integer.class);
        return count != null && count > 0;
    }

    public void updateLastSignIn(UUID userId, OffsetDateTime lastSignInAt) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", userId)
                .addValue("lastSignInAt", lastSignInAt);
        jdbcTemplate.update("UPDATE company_user SET last_sign_in_at = :lastSignInAt WHERE id = :id", params);
    }
}


