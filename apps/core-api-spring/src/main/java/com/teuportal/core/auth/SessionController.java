package com.teuportal.core.auth;

import com.teuportal.core.company.CompanyUserService;
import com.teuportal.core.company.CompanyUserSummary;
import com.teuportal.core.tenancy.TenantContextService;
import com.teuportal.core.tenancy.TenantPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class SessionController {

    private final TenantContextService tenantContextService;
    private final CompanyUserService companyUserService;
    private final String appBaseUrl;

    public SessionController(TenantContextService tenantContextService,
                             CompanyUserService companyUserService,
                             @Value("${app.base-url}") String appBaseUrl) {
        this.tenantContextService = tenantContextService;
        this.companyUserService = companyUserService;
        this.appBaseUrl = appBaseUrl;
    }

    @GetMapping(path = "/session", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SessionResponse> session() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof TenantPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<UUID> companyIdOpt = tenantContextService.currentCompanyId();
        if (companyIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UUID companyId = companyIdOpt.get();
        Optional<CompanyUserSummary> userOpt = companyUserService.findUser(companyId, principal.userId());
        if (userOpt.isEmpty()) {
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CompanyUserSummary summary = userOpt.get();
        SessionResponse response = new SessionResponse(
                summary.id(),
                summary.email(),
                summary.displayName(),
                List.of(summary.role()),
                companyId
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        if (expectsJson(request)) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, appBaseUrl + "/login")
                .build();
    }

    private static boolean expectsJson(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return true;
        }
        String requestedWith = request.getHeader("X-Requested-With");
        return requestedWith != null && "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    public record SessionResponse(
            UUID userId,
            String email,
            String name,
            List<String> roles,
            UUID companyId
    ) {
    }
}
