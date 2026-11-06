package com.project.rentmebarwaha.rental.dto;

import com.project.rentmebarwaha.rental.entity.RentalSource;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record MarkRentedRequest(
        @NotNull(message = "Rental source is required (VIA_NESTEARTH, EXTERNAL, FAMILY_FRIENDS)")
        RentalSource source,

        @NotNull(message = "Lease start date is required")
        LocalDate leaseStartDate,

        UUID tenantId,

        Boolean autoArchiveEnquiries
) {}

