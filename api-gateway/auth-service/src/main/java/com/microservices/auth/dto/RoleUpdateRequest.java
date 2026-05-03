package com.microservices.auth.dto;

import com.microservices.auth.entity.Role;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(
        @NotNull(message = "Role is required")
        Role role
) {
}
