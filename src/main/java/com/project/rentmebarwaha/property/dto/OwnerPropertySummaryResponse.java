package com.project.rentmebarwaha.property.dto;

import com.project.rentmebarwaha.property.entity.Furnishing;
import com.project.rentmebarwaha.property.entity.PropertyConfig;
import com.project.rentmebarwaha.property.entity.PropertyStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record OwnerPropertySummaryResponse(
        UUID id,
        String title,
        PropertyConfig config,
        String locality,
        String city,
        String address,
        BigDecimal monthlyRent,
        BigDecimal securityDeposit,
        Furnishing furnishing,
        PropertyStatus status,
        boolean verified,
        int viewCount,
        long visitCount,
        long enquiryCount,
        String coverImageUrl,
        List<PropertyImageDto> images,
        Set<String> amenities,
        Set<String> tags,
        Instant publishedAt,
        Instant createdAt
) {}

