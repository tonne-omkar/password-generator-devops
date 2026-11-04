package com.example.passwordgenerator;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Password length limits. Defaults live in application.properties; in Kubernetes the
 * ConfigMap overrides them through environment variables (PASSGEN_MAX_LENGTH, ...).
 */
@ConfigurationProperties(prefix = "passgen")
public record PasswordProperties(int minLength, int maxLength) {
}
