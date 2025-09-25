package com.teuportal.core.health;

import com.teuportal.core.tenancy.TenantContextService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "health", description = "Service health checks")
public class HealthController {

    private final DatabaseHealthService databaseHealthService;
    private final TenantContextService tenantContextService;

    public HealthController(DatabaseHealthService databaseHealthService,
                            TenantContextService tenantContextService) {
        this.databaseHealthService = databaseHealthService;
        this.tenantContextService = tenantContextService;
    }

    @GetMapping("/health")
    @Operation(summary = "Basic readiness probe")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/health/db")
    @Operation(summary = "Tenant-scoped database probe")
    public ResponseEntity<Map<String, Object>> databaseHealth() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "ok");
        body.put("timestamp", Instant.now().toString());
        body.put("companyId", tenantContextService.currentCompanyId().orElse(null));
        body.put("diagnostics", databaseHealthService.diagnostics());
        return ResponseEntity.ok(body);
    }
}
