package com.teuportal.core.tenancy;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class TenantContextHolder {

    private final ThreadLocal<TenantContext> current = new ThreadLocal<>();

    public void set(TenantContext context) {
        current.set(context);
    }

    public Optional<TenantContext> get() {
        return Optional.ofNullable(current.get());
    }

    public void clear() {
        current.remove();
    }
}
