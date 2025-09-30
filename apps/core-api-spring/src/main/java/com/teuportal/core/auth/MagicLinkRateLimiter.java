package com.teuportal.core.auth;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

@Component
public class MagicLinkRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int MAX_REQUESTS = 5;

    private final Map<String, WindowState> attempts = new ConcurrentHashMap<>();

    public boolean allow(String ipAddress, String email) {
        String key = buildKey(ipAddress, email);
        long now = System.currentTimeMillis();
        AtomicBoolean permitted = new AtomicBoolean(true);
        attempts.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart() >= WINDOW.toMillis()) {
                permitted.set(true);
                return new WindowState(now, 1);
            }
            if (existing.count() >= MAX_REQUESTS) {
                permitted.set(false);
                return existing;
            }
            permitted.set(true);
            return new WindowState(existing.windowStart(), existing.count() + 1);
        });
        return permitted.get();
    }

    private static String buildKey(String ipAddress, String email) {
        String ip = ipAddress == null ? "unknown" : ipAddress.trim().toLowerCase(Locale.ROOT);
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return ip + "|" + normalizedEmail;
    }

    private record WindowState(long windowStart, int count) {
    }
}
