package com.teuportal.core.storage;

import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FolderRepository {

    private static final RowMapper<FolderSummary> ROW_MAPPER = (rs, rowNum) -> new FolderSummary(
            rs.getObject("id", java.util.UUID.class),
            rs.getString("name"),
            rs.getObject("parent_id", java.util.UUID.class),
            rs.getObject("created_at", java.time.OffsetDateTime.class)
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FolderRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FolderSummary> findRootFolders(int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("limit", limit);
        return jdbcTemplate.query("""
                SELECT id, name, parent_id, created_at
                FROM folder
                WHERE parent_id IS NULL
                ORDER BY created_at DESC
                LIMIT :limit
                """, params, ROW_MAPPER);
    }
}
