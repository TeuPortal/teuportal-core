package com.teuportal.core.company;

import java.util.List;
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
}

