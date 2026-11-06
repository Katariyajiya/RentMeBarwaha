package com.project.rentmebarwaha.property.dto;

import com.project.rentmebarwaha.property.entity.PropertyImage;

import java.util.UUID;

public record PropertyImageDto(
        UUID id,
        String url,
        int sortOrder,
        boolean isCover
) {
    public static PropertyImageDto fromEntity(PropertyImage image) {
        if (image == null) return null;
        return new PropertyImageDto(
                image.getId(),
                image.getUrl(),
                image.getSortOrder(),
                image.isCover()
        );
    }
}

