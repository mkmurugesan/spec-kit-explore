package com.example.usermanagement.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private Duration accessTokenExpiry;
    private Duration refreshTokenExpiry;

    @PostConstruct
    public void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret must be configured");
        }
        // HS256 requires a minimum of 256 bits (32 bytes)
        if (secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 characters (256 bits) for HS256");
        }
    }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public Duration getAccessTokenExpiry() { return accessTokenExpiry; }
    public void setAccessTokenExpiry(Duration accessTokenExpiry) { this.accessTokenExpiry = accessTokenExpiry; }
    public Duration getRefreshTokenExpiry() { return refreshTokenExpiry; }
    public void setRefreshTokenExpiry(Duration refreshTokenExpiry) { this.refreshTokenExpiry = refreshTokenExpiry; }
}

