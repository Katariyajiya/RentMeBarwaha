package com.project.rentmebarwaha.enquiry.dto;

import com.project.rentmebarwaha.enquiry.entity.EnquiryStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEnquiryStatusRequest(
        @NotNull(message = "Enquiry status is required")
        EnquiryStatus status
) {}

