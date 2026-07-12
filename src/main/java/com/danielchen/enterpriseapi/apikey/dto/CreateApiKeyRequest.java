package com.danielchen.enterpriseapi.apikey.dto;

import com.danielchen.enterpriseapi.tenant.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateApiKeyRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Role is required")
        Role role
) {}
