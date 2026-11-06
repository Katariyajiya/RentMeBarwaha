package com.project.rentmebarwaha.property.dto;

import com.project.rentmebarwaha.property.entity.Furnishing;
import com.project.rentmebarwaha.property.entity.PreferredTenants;
import com.project.rentmebarwaha.property.entity.PropertyConfig;
import com.project.rentmebarwaha.property.entity.PropertyStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PropertyDetailResponse(
        UUID id,
        String title,
        String description,
        PropertyConfig config,
        String locality,
        String city,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer areaSqft,
        Integer floor,
        BigDecimal monthlyRent,
        BigDecimal securityDeposit,
        Furnishing furnishing,
        PreferredTenants preferredTenants,
        LocalDate availableFrom,
        boolean availableImmediately,
        boolean directOwner,
        boolean featured,
        boolean verified,
        PropertyStatus status,
        int viewCount,
        Instant publishedAt,
        Set<String> amenities,
        Set<String> tags,
        List<PropertyImageDto> images,
        OwnerCardDto ownerCard,
        boolean savedByMe
) {}

