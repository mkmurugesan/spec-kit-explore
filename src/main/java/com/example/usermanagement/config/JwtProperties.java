package com.example.usermanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private Duration accessTokenExpiry;
    private Duration refreshTokenExpiry;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public Duration getAccessTokenExpiry() { return accessTokenExpiry; }
    public void setAccessTokenExpiry(Duration accessTokenExpiry) { this.accessTokenExpiry = accessTokenExpiry; }
    public Duration getRefreshTokenExpiry() { return refreshTokenExpiry; }
    public void setRefreshTokenExpiry(Duration refreshTokenExpiry) { this.refreshTokenExpiry = refreshTokenExpiry; }
}

