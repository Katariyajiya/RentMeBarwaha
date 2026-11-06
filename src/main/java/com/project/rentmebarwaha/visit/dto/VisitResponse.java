package com.project.rentmebarwaha.visit.dto;

import com.project.rentmebarwaha.visit.entity.Visit;
import com.project.rentmebarwaha.visit.entity.VisitStatus;

import java.time.Instant;
import java.util.UUID;

public record VisitResponse(
        UUID id,
        UUID enquiryId,
        UUID propertyId,
        String propertyTitle,
        String propertyLocality,
        UUID tenantId,
        String tenantName,
        String tenantMobile,
        UUID ownerId,
        String ownerName,
        Instant scheduledAt,
        VisitStatus status,
        String notes,
        Instant createdAt
) {
    public static VisitResponse fromEntity(Visit v) {
        if (v == null) return null;
        return new VisitResponse(
                v.getId(),
                v.getEnquiry() != null ? v.getEnquiry().getId() : null,
                v.getProperty() != null ? v.getProperty().getId() : null,
                v.getProperty() != null ? v.getProperty().getTitle() : null,
                v.getProperty() != null ? v.getProperty().getLocality() : null,
                v.getTenant() != null ? v.getTenant().getId() : null,
                v.getTenant() != null ? v.getTenant().getName() : null,
                v.getTenant() != null ? v.getTenant().getPhone() : null,
                v.getOwner() != null ? v.getOwner().getId() : null,
                v.getOwner() != null ? v.getOwner().getName() : null,
                v.getScheduledAt(),
                v.getStatus(),
                v.getNotes(),
                v.getCreatedAt()
        );
    }
}

