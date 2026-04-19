package com.example.usermanagement.dto;

import java.time.Instant;
import java.util.UUID;

public record SignupResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String status,
        Instant createdAt
) {}

