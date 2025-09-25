package com.teuportal.core.tenancy;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@org.springframework.context.annotation.DependsOn("singleCompanyBootstrapper")
public class SingleCompanyContextResolver implements CompanyContextResolver {

    private static final Logger log = LoggerFactory.getLogger(SingleCompanyContextResolver.class);

    private final JdbcTemplate jdbcTemplate;
    private volatile UUID cachedCompanyId;

    public SingleCompanyContextResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void hydrate() {
        List<UUID> ids = jdbcTemplate.query(
                "SELECT id FROM company WHERE is_active = true ORDER BY created_at ASC LIMIT 1",
                (rs, rowNum) -> rs.getObject("id", java.util.UUID.class)
        );
        if (ids.isEmpty()) {
            throw new IllegalStateException("No active company found. Seed a company row before starting the API.");
        }
        this.cachedCompanyId = ids.getFirst();
        log.info("Resolved single-company deployment using company id {}", cachedCompanyId);
    }

    @Override
    public UUID resolveCompanyId(HttpServletRequest request) {
        if (cachedCompanyId == null) {
            throw new IllegalStateException("Company context not initialised");
        }
        return cachedCompanyId;
    }
}

