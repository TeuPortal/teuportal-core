package com.teuportal.core.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class OAuthSignInController {

    @GetMapping("/signin")
    public ResponseEntity<Void> redirectToProvider(@RequestParam(name = "provider", defaultValue = "google") String provider,
                                                   HttpServletRequest request) {
        String registrationId = normalizeProvider(provider);
        URI redirectUri = URI.create(request.getContextPath() + "/oauth2/authorization/" + registrationId);
        return ResponseEntity.status(302).location(redirectUri).build();
    }

    private static String normalizeProvider(String provider) {
        if (!StringUtils.hasText(provider)) {
            return "google";
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }
}
