package com.project.rentmebarwaha.agreement.dto;

import com.project.rentmebarwaha.agreement.entity.Agreement;
import com.project.rentmebarwaha.agreement.entity.AgreementStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AgreementResponse(
        UUID id,
        UUID propertyId,
        String propertyTitle,
        String propertyLocality,
        UUID ownerId,
        String ownerName,
        UUID tenantId,
        String tenantName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal rent,
        BigDecimal deposit,
        String documentUrl,
        AgreementStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static AgreementResponse fromEntity(Agreement a) {
        if (a == null) return null;
        return new AgreementResponse(
                a.getId(),
                a.getProperty() != null ? a.getProperty().getId() : null,
                a.getProperty() != null ? a.getProperty().getTitle() : null,
                a.getProperty() != null ? a.getProperty().getLocality() : null,
                a.getOwner() != null ? a.getOwner().getId() : null,
                a.getOwner() != null ? a.getOwner().getName() : null,
                a.getTenant() != null ? a.getTenant().getId() : null,
                a.getTenant() != null ? a.getTenant().getName() : null,
                a.getStartDate(),
                a.getEndDate(),
                a.getRent(),
                a.getDeposit(),
                a.getDocumentUrl(),
                a.getStatus(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}

