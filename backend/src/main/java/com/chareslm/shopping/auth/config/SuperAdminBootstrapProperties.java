package com.chareslm.shopping.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.bootstrap-super-admin")
public record SuperAdminBootstrapProperties(boolean enabled, String username, String password) {
}
