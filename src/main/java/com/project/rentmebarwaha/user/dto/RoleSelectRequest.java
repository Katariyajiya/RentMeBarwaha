package com.project.rentmebarwaha.user.dto;

import com.project.rentmebarwaha.user.entity.Role;
import jakarta.validation.constraints.NotNull;

public record RoleSelectRequest(
        @NotNull(message = "Role is required (TENANT or OWNER)")
        Role role
) {}

