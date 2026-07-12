package com.danielchen.enterpriseapi.security;

import com.danielchen.enterpriseapi.tenant.Role;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, UUID orgId, Role role) {}
