package com.project.rentmebarwaha.property.dto;

import com.project.rentmebarwaha.property.entity.Furnishing;
import com.project.rentmebarwaha.property.entity.PropertyConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record PropertyListItemResponse(
        UUID id,
        String title,
        String coverImageUrl,
        BigDecimal monthlyRent,
        BigDecimal securityDeposit,
        PropertyConfig config,
        Furnishing furnishing,
        Integer areaSqft,
        String locality,
        String city,
        Set<String> tags,
        boolean directOwner,
        boolean verified,
        boolean savedByMe,
        int viewCount,
        boolean availableImmediately,
        LocalDate availableFrom
) {}

