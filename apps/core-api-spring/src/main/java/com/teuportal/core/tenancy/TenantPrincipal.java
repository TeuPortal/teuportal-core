package com.teuportal.core.tenancy;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public record TenantPrincipal(UUID userId) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}