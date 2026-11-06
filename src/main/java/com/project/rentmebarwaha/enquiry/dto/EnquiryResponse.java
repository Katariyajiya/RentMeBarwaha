package com.project.rentmebarwaha.enquiry.dto;

import com.project.rentmebarwaha.enquiry.entity.Enquiry;
import com.project.rentmebarwaha.enquiry.entity.EnquiryStatus;
import com.project.rentmebarwaha.enquiry.entity.LeaseDuration;
import com.project.rentmebarwaha.enquiry.entity.TenantType;
import com.project.rentmebarwaha.enquiry.entity.VisitTiming;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record EnquiryResponse(
        UUID id,
        UUID propertyId,
        String propertyTitle,
        UUID tenantId,
        String fullName,
        String mobile,
        LocalDate preferredMoveInDate,
        TenantType tenantType,
        LeaseDuration leaseDuration,
        Set<VisitTiming> visitTimings,
        String personalNote,
        EnquiryStatus status,
        String ownerPhone, // Exposed ONLY after enquiry is ACCEPTED
        Instant createdAt
) {
    public static EnquiryResponse fromEntity(Enquiry e, boolean revealOwnerPhone) {
        if (e == null) return null;
        String phone = null;
        if (revealOwnerPhone && e.getStatus() == EnquiryStatus.ACCEPTED && e.getProperty() != null && e.getProperty().getOwner() != null) {
            phone = e.getProperty().getOwner().getPhone();
        }

        return new EnquiryResponse(
                e.getId(),
                e.getProperty() != null ? e.getProperty().getId() : null,
                e.getProperty() != null ? e.getProperty().getTitle() : null,
                e.getTenant() != null ? e.getTenant().getId() : null,
                e.getFullName(),
                e.getMobile(),
                e.getPreferredMoveInDate(),
                e.getTenantType(),
                e.getLeaseDuration(),
                e.getVisitTimings(),
                e.getPersonalNote(),
                e.getStatus(),
                phone,
                e.getCreatedAt()
        );
    }
}

