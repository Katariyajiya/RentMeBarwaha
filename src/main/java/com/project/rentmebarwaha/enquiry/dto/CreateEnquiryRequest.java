package com.project.rentmebarwaha.enquiry.dto;

import com.project.rentmebarwaha.enquiry.entity.LeaseDuration;
import com.project.rentmebarwaha.enquiry.entity.TenantType;
import com.project.rentmebarwaha.enquiry.entity.VisitTiming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Set;

public record CreateEnquiryRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Mobile number is required")
        String mobile,

        @NotNull(message = "Preferred move-in date is required")
        LocalDate preferredMoveInDate,

        @NotNull(message = "Tenant type is required")
        TenantType tenantType,

        @NotNull(message = "Lease duration is required")
        LeaseDuration leaseDuration,

        @NotEmpty(message = "At least one visit timing must be selected")
        Set<VisitTiming> visitTimings,

        String personalNote
) {}

