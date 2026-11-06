package com.project.rentmebarwaha.user.dto;

import com.project.rentmebarwaha.user.entity.Role;
import com.project.rentmebarwaha.user.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String name,
        String phone,
        String email,
        Role role,
        String avatarUrl,
        String city,
        boolean kycVerified,
        boolean ownerVerified,
        boolean active,
        Instant createdAt
) {
    public static UserProfileResponse fromEntity(User user) {
        if (user == null) return null;
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getCity(),
                user.isKycVerified(),
                user.isOwnerVerified(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}

