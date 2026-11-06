package com.project.rentmebarwaha.agreement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateAgreementRequest(
        @NotNull(message = "Property ID is required")
        UUID propertyId,

        @NotNull(message = "Tenant ID is required")
        UUID tenantId,

        @NotNull(message = "Lease start date is required")
        LocalDate startDate,

        @NotNull(message = "Lease end date is required")
        LocalDate endDate,

        @NotNull(message = "Monthly rent is required")
        @Positive(message = "Rent must be greater than zero")
        BigDecimal rent,

        BigDecimal deposit,

        String documentUrl
) {}

