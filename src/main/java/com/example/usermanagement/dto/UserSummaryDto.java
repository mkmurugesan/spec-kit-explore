package com.example.usermanagement.dto;

import java.time.Instant;
import java.util.UUID;

public record UserSummaryDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String status,
        String role,
        Instant createdAt
) {}

