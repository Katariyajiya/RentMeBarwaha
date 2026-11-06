package com.project.rentmebarwaha.property.dto;

import com.project.rentmebarwaha.property.entity.Furnishing;
import com.project.rentmebarwaha.property.entity.PreferredTenants;
import com.project.rentmebarwaha.property.entity.PropertyConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record UpdatePropertyRequest(
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
        Boolean availableImmediately,
        Boolean directOwner,
        Set<String> amenities,
        Set<String> tags
) {}

