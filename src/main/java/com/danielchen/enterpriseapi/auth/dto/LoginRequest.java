package com.danielchen.enterpriseapi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record LoginRequest(
        @NotBlank @Email
        String email,

        @NotBlank
        String password,

        // Required only when the user belongs to multiple organizations
        UUID orgId
) {}
