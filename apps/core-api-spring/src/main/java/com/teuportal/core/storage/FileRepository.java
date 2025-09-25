package com.teuportal.core.storage;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FileRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FileRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countFiles() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM file", new java.util.HashMap<>(), Long.class);
        return count == null ? 0L : count;
    }
}
