package com.teuportal.core.company;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompanyUserService {


    private final CompanyUserRepository repository;
    private final Clock clock;

    public CompanyUserService(CompanyUserRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Optional<CompanyUserSummary> findByEmail(UUID companyId, String email) {
        String normalizedEmail = normalizeEmail(email);
        if (!StringUtils.hasText(normalizedEmail)) {
            return Optional.empty();
        }
        return repository.findByEmail(companyId, normalizedEmail);
    }

    public Optional<CompanyUserSummary> findUser(UUID companyId, UUID userId) {
        return repository.findById(companyId, userId);
    }

    public void updateLastSignIn(UUID userId) {
        repository.updateLastSignIn(userId, OffsetDateTime.now(clock));
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}