package com.project.rentmebarwaha.rental.dto;

import com.project.rentmebarwaha.rental.entity.RentalRecord;
import com.project.rentmebarwaha.rental.entity.RentalSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RentalRecordResponse(
        UUID id,
        UUID propertyId,
        String propertyTitle,
        UUID tenantId,
        String tenantName,
        RentalSource source,
        LocalDate leaseStartDate,
        boolean autoArchiveEnquiries,
        BigDecimal platformFeePercent,
        Instant createdAt
) {
    public static RentalRecordResponse fromEntity(RentalRecord r) {
        if (r == null) return null;
        return new RentalRecordResponse(
                r.getId(),
                r.getProperty() != null ? r.getProperty().getId() : null,
                r.getProperty() != null ? r.getProperty().getTitle() : null,
                r.getTenant() != null ? r.getTenant().getId() : null,
                r.getTenant() != null ? r.getTenant().getName() : null,
                r.getSource(),
                r.getLeaseStartDate(),
                r.isAutoArchiveEnquiries(),
                r.getPlatformFeePercent(),
                r.getCreatedAt()
        );
    }
}

