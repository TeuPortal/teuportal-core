package com.teuportal.core.tenancy;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public interface CompanyContextResolver {
    UUID resolveCompanyId(HttpServletRequest request);
}
