package com.project.rentmebarwaha.property.dto;

import com.project.rentmebarwaha.user.entity.User;

import java.util.UUID;

public record OwnerCardDto(
        UUID id,
        String name,
        String avatarUrl,
        boolean ownerVerified
) {
    public static OwnerCardDto fromEntity(User owner) {
        if (owner == null) return null;
        return new OwnerCardDto(
                owner.getId(),
                owner.getName() != null ? owner.getName() : "Property Owner",
                owner.getAvatarUrl(),
                owner.isOwnerVerified()
        );
    }
}

