package com.teuportal.core.tenancy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class TenantContext {

    private final UUID companyId;
    private final UUID userId;
    private final Set<String> roles;

    public TenantContext(UUID companyId, UUID userId, Set<String> roles) {
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.userId = userId;
        this.roles = roles == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(roles));
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public boolean hasRole(String role) {
        return role != null && roles.contains(role);
    }
}
