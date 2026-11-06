package com.project.rentmebarwaha.property.dto;

import com.project.rentmebarwaha.property.entity.Furnishing;
import com.project.rentmebarwaha.property.entity.PreferredTenants;
import com.project.rentmebarwaha.property.entity.PropertyConfig;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record CreatePropertyRequest(
        @NotBlank(message = "Property title is required")
        String title,

        String description,

        @NotNull(message = "Property configuration (BHK) is required")
        PropertyConfig config,

        @NotBlank(message = "Locality is required")
        String locality,

        String city,

        @NotBlank(message = "Address is required")
        String address,

        BigDecimal latitude,
        BigDecimal longitude,
        Integer areaSqft,
        Integer floor,

        @NotNull(message = "Monthly rent is required")
        @DecimalMin(value = "1.0", message = "Monthly rent must be greater than zero")
        BigDecimal monthlyRent,

        BigDecimal securityDeposit,

        @NotNull(message = "Furnishing status is required")
        Furnishing furnishing,

        PreferredTenants preferredTenants,
        LocalDate availableFrom,
        Boolean availableImmediately,
        Boolean directOwner,
        Set<String> amenities,
        Set<String> tags
) {}

