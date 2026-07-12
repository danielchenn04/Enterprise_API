package com.danielchen.enterpriseapi.auth.dto;

import com.danielchen.enterpriseapi.tenant.Role;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        UUID orgId,
        Role role
) {}
