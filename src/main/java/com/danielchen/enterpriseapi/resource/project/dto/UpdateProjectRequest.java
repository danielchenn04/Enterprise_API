package com.danielchen.enterpriseapi.resource.project.dto;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        String description
) {}
