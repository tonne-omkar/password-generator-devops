package com.example.passwordgenerator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple health check kept from the first version. Kubernetes probes use the Actuator
 * endpoints (/actuator/health/liveness and /actuator/health/readiness) instead.
 */
@RestController
@RequestMapping("/api/password")
public class HealthController {

    private final String version;

    public HealthController(@Value("${app.version:dev}") String version) {
        this.version = version;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "Password Generator API",
                "version", version
        );
    }
}
