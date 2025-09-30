package com.teuportal.core.auth;

import com.teuportal.core.company.CompanyUserSummary;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Validated
public class MagicLinkController {

    private static final Logger log = LoggerFactory.getLogger(MagicLinkController.class);

    private final MagicLinkService magicLinkService;
    private final SessionAuthenticationService sessionAuthenticationService;
    private final MagicLinkRateLimiter rateLimiter;
    private final String appBaseUrl;

    public MagicLinkController(MagicLinkService magicLinkService,
                               SessionAuthenticationService sessionAuthenticationService,
                               MagicLinkRateLimiter rateLimiter,
                               @Value("${app.base-url}") String appBaseUrl) {
        this.magicLinkService = magicLinkService;
        this.sessionAuthenticationService = sessionAuthenticationService;
        this.rateLimiter = rateLimiter;
        this.appBaseUrl = appBaseUrl;
    }

    @PostMapping(path = "/email", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> requestMagicLink(@Valid @RequestBody MagicLinkRequest request,
                                                                HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        if (!rateLimiter.allow(clientIp, request.email())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("status", "error", "message", "Too many requests. Please wait before trying again."));
        }

        magicLinkService.requestMagicLink(request.email());
        return ResponseEntity.ok(Map.of("message", "If the email exists, we have sent a sign-in link."));
    }

    @GetMapping(path = "/magic")
    public ResponseEntity<?> consumeMagicLink(@RequestParam("token") String token,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        boolean expectsJson = expectsJson(request);
        try {
            String email = magicLinkService.consumeMagicLink(token);
            Optional<CompanyUserSummary> user = sessionAuthenticationService.signInByEmail(email, request, response);
            if (user.isEmpty()) {
                log.warn("Magic link sign-in attempted for unknown user {}", email);
                return handleUnknownUser(expectsJson);
            }
            log.info("Magic link sign-in succeeded for user {}", user.get().email());
            if (expectsJson) {
                return ResponseEntity.ok(Map.of("status", "ok"));
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, appBaseUrl + "/app")
                    .build();
        } catch (MagicLinkService.InvalidMagicLinkException ex) {
            log.warn("Magic link consumption failed: {}", ex.getMessage());
            return handleUnknownUser(expectsJson);
        }
    }

    private ResponseEntity<?> handleUnknownUser(boolean expectsJson) {
        if (expectsJson) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Magic link is invalid or expired."));
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(appBaseUrl + "/login?error=magic_link"))
                .build();
    }

    private static String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int commaIndex = forwarded.indexOf(',');
            return commaIndex > -1 ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }

    private static boolean expectsJson(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return true;
        }
        String requestedWith = request.getHeader("X-Requested-With");
        return requestedWith != null && "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    public record MagicLinkRequest(@NotBlank @Email String email) {
    }
}