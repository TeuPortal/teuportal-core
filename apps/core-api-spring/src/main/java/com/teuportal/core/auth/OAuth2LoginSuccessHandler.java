package com.teuportal.core.auth;

import com.teuportal.core.company.CompanyUserService;
import com.teuportal.core.company.CompanyUserSummary;
import com.teuportal.core.tenancy.CompanyContextResolver;
import com.teuportal.core.tenancy.TenantContextService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final TenantContextService tenantContextService;
    private final CompanyContextResolver companyContextResolver;
    private final CompanyUserService companyUserService;
    private final ExternalAccountRepository externalAccountRepository;
    private final SessionAuthenticationService sessionAuthenticationService;
    private final Clock clock;
    private final String appBaseUrl;

    public OAuth2LoginSuccessHandler(TenantContextService tenantContextService,
                                     CompanyContextResolver companyContextResolver,
                                     CompanyUserService companyUserService,
                                     ExternalAccountRepository externalAccountRepository,
                                     SessionAuthenticationService sessionAuthenticationService,
                                     Clock clock,
                                     @Value("${app.base-url}") String appBaseUrl) {
        this.tenantContextService = tenantContextService;
        this.companyContextResolver = companyContextResolver;
        this.companyUserService = companyUserService;
        this.externalAccountRepository = externalAccountRepository;
        this.sessionAuthenticationService = sessionAuthenticationService;
        this.clock = clock;
        this.appBaseUrl = appBaseUrl;
        setDefaultTargetUrl(appBaseUrl + "/app");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        OAuth2User oauth2User = token.getPrincipal();
        String email = extractEmail(oauth2User);
        String providerUserId = extractSubject(oauth2User);

        if (!StringUtils.hasText(email) || !StringUtils.hasText(providerUserId)) {
            log.warn("OAuth2 login missing required attributes (email or subject). provider={} principal={}",
                    token.getAuthorizedClientRegistrationId(), oauth2User.getAttributes());
            handleFailureResponse(request, response);
            return;
        }

        UUID companyId = tenantContextService.currentCompanyId()
                .orElseGet(() -> companyContextResolver.resolveCompanyId(request));

        Optional<CompanyUserSummary> user = companyUserService.findByEmail(companyId, email);
        if (user.isEmpty()) {
            log.warn("OAuth2 login rejected for unknown user {}", email);
            handleFailureResponse(request, response);
            return;
        }

        CompanyUserSummary summary = user.get();
        companyUserService.updateLastSignIn(summary.id());
        externalAccountRepository.upsert(companyId,
                summary.id(),
                token.getAuthorizedClientRegistrationId(),
                providerUserId,
                email,
                OffsetDateTime.now(clock));

        sessionAuthenticationService.storeAuthentication(summary, request, response);

        if (expectsJson(request)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"status\":\"ok\"}");
            clearAuthenticationAttributes(request);
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    private static String extractEmail(OAuth2User user) {
        Object email = user.getAttributes().get("email");
        return email == null ? null : email.toString().trim().toLowerCase(Locale.ROOT);
    }

    private static String extractSubject(OAuth2User user) {
        Map<String, Object> attributes = user.getAttributes();
        Object subject = attributes.get("sub");
        if (subject == null) {
            subject = user.getName();
        }
        return subject == null ? null : subject.toString();
    }

    private static boolean expectsJson(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return true;
        }
        String requestedWith = request.getHeader("X-Requested-With");
        return requestedWith != null && "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    private void handleFailureResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (expectsJson(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Unable to complete login\"}");
        } else {
            response.sendRedirect(appBaseUrl + "/login?error=oauth2");
        }
    }
}