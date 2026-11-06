package com.project.rentmebarwaha.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        String avatarUrl,

        @Size(max = 100, message = "City must not exceed 100 characters")
        String city
) {}

