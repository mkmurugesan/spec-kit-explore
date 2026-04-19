package com.example.usermanagement.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminCredentialsValidator {

    @Value("${admin.master.username:}")
    private String username;

    @Value("${admin.master.password:}")
    private String password;

    @PostConstruct
    public void validate() {
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "admin.master.username and admin.master.password must be configured");
        }
    }
}

