package com.teuportal.core.health;

import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "health", description = "Service health checks")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Basic readiness probe")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "timestamp", Instant.now().toString()
        ));
    }
}
