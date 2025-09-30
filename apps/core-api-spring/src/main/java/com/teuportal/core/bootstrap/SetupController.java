package com.teuportal.core.bootstrap;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/setup")
@Tag(name = "Setup", description = "Initial server setup for single-company deployments")
public class SetupController {

    @GetMapping
    @Operation(summary = "Check setup status", description = "Returns the current setup status. Placeholder endpoint.")
    public ResponseEntity<Void> getSetupStatus() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}