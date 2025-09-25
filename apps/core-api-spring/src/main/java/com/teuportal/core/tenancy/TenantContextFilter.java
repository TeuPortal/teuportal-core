package com.teuportal.core.tenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    private final CompanyContextResolver companyContextResolver;
    private final TenantContextHolder contextHolder;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public TenantContextFilter(CompanyContextResolver companyContextResolver,
                               TenantContextHolder contextHolder,
                               JdbcTemplate jdbcTemplate,
                               PlatformTransactionManager transactionManager) {
        this.companyContextResolver = companyContextResolver;
        this.contextHolder = contextHolder;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        UUID companyId = companyContextResolver.resolveCompanyId(request);
        TenantContext context = new TenantContext(companyId, resolveUserId(), resolveRoles());

        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    contextHolder.set(context);
                    applySessionVariables(context);
                    try {
                        filterChain.doFilter(request, response);
                    } catch (IOException | ServletException e) {
                        // Wrap checked exceptions to propagate outside the transaction callback
                        throw new TenantFilterException(e);
                    } finally {
                        contextHolder.clear();
                    }
                }
            });
        } catch (TenantFilterException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ServletException servletEx) {
                throw servletEx;
            }
            if (cause instanceof IOException ioEx) {
                throw ioEx;
            }
            throw e;
        }
    }

    private UUID resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof TenantPrincipal tenantPrincipal) {
            return tenantPrincipal.userId();
        }
        // TODO: replace with real principal extraction when session auth is implemented
        return null;
    }

    private Set<String> resolveRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptySet();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
    }

    private void applySessionVariables(TenantContext context) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET LOCAL app.company_id = '" + context.getCompanyId() + "'");
                String userValue = context.getUserId() == null ? "" : context.getUserId().toString();
                statement.execute("SET LOCAL app.user_id = '" + userValue + "'");
            } catch (SQLException ex) {
                log.error("Failed to apply tenant session variables", ex);
                throw ex;
            }
            return null;
        });
    }

    private static class TenantFilterException extends RuntimeException {
        TenantFilterException(Throwable cause) {
            super(cause);
        }
    }
}



