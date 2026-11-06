package com.project.rentmebarwaha.visit.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ScheduleVisitRequest(
        @NotNull(message = "Scheduled visit timestamp is required")
        @Future(message = "Visit must be scheduled for a future date and time")
        Instant scheduledAt,

        String notes
) {}

