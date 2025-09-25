package com.teuportal.core.company;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CompanyUserSummary(
        UUID id,
        String email,
        String displayName,
        String role,
        OffsetDateTime joinedAt
) {
}
