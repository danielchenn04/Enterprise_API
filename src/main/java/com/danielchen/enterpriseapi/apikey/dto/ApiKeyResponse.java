package com.danielchen.enterpriseapi.apikey.dto;

import com.danielchen.enterpriseapi.apikey.ApiKey;
import com.danielchen.enterpriseapi.tenant.Role;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyResponse(
        UUID id,
        String name,
        String keyHint,
        Role role,
        boolean active,
        Instant createdAt,
        Instant revokedAt
) {
    public static ApiKeyResponse from(ApiKey key) {
        return new ApiKeyResponse(
                key.getId(),
                key.getName(),
                key.getKeyHint(),
                key.getRole(),
                key.isActive(),
                key.getCreatedAt(),
                key.getRevokedAt()
        );
    }
}
