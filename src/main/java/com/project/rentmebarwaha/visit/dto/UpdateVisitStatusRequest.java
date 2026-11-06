package com.project.rentmebarwaha.visit.dto;

import com.project.rentmebarwaha.visit.entity.VisitStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateVisitStatusRequest(
        @NotNull(message = "Visit status is required")
        VisitStatus status,

        String notes
) {}

