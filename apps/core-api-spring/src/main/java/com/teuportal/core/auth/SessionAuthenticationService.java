package com.teuportal.core.auth;

import com.teuportal.core.company.CompanyUserService;
import com.teuportal.core.company.CompanyUserSummary;
import com.teuportal.core.tenancy.TenantContextService;
import com.teuportal.core.tenancy.TenantPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
public class SessionAuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(SessionAuthenticationService.class);

    private final TenantContextService tenantContextService;
    private final CompanyUserService companyUserService;
    private final HttpSessionSecurityContextRepository securityContextRepository;

    public SessionAuthenticationService(TenantContextService tenantContextService,
                                        CompanyUserService companyUserService) {
        this.tenantContextService = tenantContextService;
        this.companyUserService = companyUserService;
        this.securityContextRepository = new HttpSessionSecurityContextRepository();
    }

    public Optional<CompanyUserSummary> signInByEmail(String email, HttpServletRequest request, HttpServletResponse response) {
        UUID companyId = tenantContextService.currentCompanyId()
                .orElseThrow(() -> new IllegalStateException("Company context must be available"));
        Optional<CompanyUserSummary> user = companyUserService.findByEmail(companyId, email);
        user.ifPresent(summary -> {
            companyUserService.updateLastSignIn(summary.id());
            storeAuthentication(summary, request, response);
        });
        if (user.isEmpty()) {
            log.warn("Attempted sign-in for unknown user '{}' in company {}", email, companyId);
        }
        return user;
    }

    public void storeAuthentication(CompanyUserSummary user, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new TenantPrincipal(user.id()),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.role()))
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true);
        securityContextRepository.saveContext(context, request, response);
        log.debug("Stored session security context for user {}", user.id());
    }
}