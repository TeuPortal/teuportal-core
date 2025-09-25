package com.teuportal.core.tenancy;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class TenantContextService {

    private final TenantContextHolder holder;

    public TenantContextService(TenantContextHolder holder) {
        this.holder = holder;
    }

    public TenantContext currentContext() {
        return holder.get().orElseThrow(() -> new IllegalStateException("Tenant context is not available for this thread"));
    }

    public Optional<UUID> currentCompanyId() {
        return holder.get().map(TenantContext::getCompanyId);
    }

    public Optional<UUID> currentUserId() {
        return holder.get().map(TenantContext::getUserId);
    }
}
