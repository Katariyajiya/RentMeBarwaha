package com.project.rentmebarwaha.auth.dto;

import com.project.rentmebarwaha.user.dto.UserSummaryDto;
import com.project.rentmebarwaha.user.entity.Role;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        boolean isNewUser,
        Role role,
        UserSummaryDto user
) {}

