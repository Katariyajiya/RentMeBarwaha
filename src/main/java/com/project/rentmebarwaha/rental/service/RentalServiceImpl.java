package com.project.rentmebarwaha.rental.service;

import com.project.rentmebarwaha.common.exception.BadRequestException;
import com.project.rentmebarwaha.common.exception.ForbiddenException;
import com.project.rentmebarwaha.common.exception.ResourceNotFoundException;
import com.project.rentmebarwaha.enquiry.entity.Enquiry;
import com.project.rentmebarwaha.enquiry.entity.EnquiryStatus;
import com.project.rentmebarwaha.enquiry.repository.EnquiryRepository;
import com.project.rentmebarwaha.notification.entity.NotificationType;
import com.project.rentmebarwaha.notification.service.NotificationService;
import com.project.rentmebarwaha.property.entity.Property;
import com.project.rentmebarwaha.property.entity.PropertyStatus;
import com.project.rentmebarwaha.property.repository.PropertyRepository;
import com.project.rentmebarwaha.rental.dto.MarkRentedRequest;
import com.project.rentmebarwaha.rental.dto.RentalRecordResponse;
import com.project.rentmebarwaha.rental.entity.RentalRecord;
import com.project.rentmebarwaha.rental.entity.RentalSource;
import com.project.rentmebarwaha.rental.repository.RentalRecordRepository;
import com.project.rentmebarwaha.user.entity.User;
import com.project.rentmebarwaha.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentalServiceImpl implements RentalService {

    private final RentalRecordRepository rentalRecordRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final EnquiryRepository enquiryRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public RentalRecordResponse markPropertyAsRented(UUID ownerId, UUID propertyId, MarkRentedRequest request) {
        Property property = propertyRepository.findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", "id", propertyId));

        if (!property.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not have permission to mark this property as rented");
        }

        if (property.getStatus() == PropertyStatus.RENTED) {
            throw new BadRequestException("Property is already marked as RENTED");
        }

        User tenant = null;
        if (request.tenantId() != null) {
            tenant = userRepository.findByIdAndDeletedAtIsNull(request.tenantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", request.tenantId()));
        }

        RentalRecord record = RentalRecord.builder()
                .property(property)
                .tenant(tenant)
                .source(request.source())
                .leaseStartDate(request.leaseStartDate())
                .autoArchiveEnquiries(Boolean.TRUE.equals(request.autoArchiveEnquiries()))
                .platformFeePercent(BigDecimal.ZERO) // 0.00% Zero brokerage platform fee
                .build();

        RentalRecord savedRecord = rentalRecordRepository.save(record);

        // Update property listing status to RENTED
        property.setStatus(PropertyStatus.RENTED);
        propertyRepository.save(property);
        log.info("Property {} marked as RENTED by owner {}", propertyId, ownerId);

        // Optionally archive active enquiries for this property
        if (Boolean.TRUE.equals(request.autoArchiveEnquiries())) {
            List<Enquiry> activeEnquiries = enquiryRepository.findByPropertyIdAndStatusIn(
                    propertyId,
                    List.of(EnquiryStatus.NEW, EnquiryStatus.REPLIED, EnquiryStatus.VISIT_SCHEDULED)
            );
            for (Enquiry e : activeEnquiries) {
                e.setStatus(EnquiryStatus.CLOSED);
            }
            enquiryRepository.saveAll(activeEnquiries);
            log.info("Auto-archived {} open enquiries for rented property {}", activeEnquiries.size(), propertyId);
        }

        // Send notification to tenant if rented via platform
        if (tenant != null && request.source() == RentalSource.VIA_NESTEARTH) {
            notificationService.sendNotification(
                    tenant.getId(),
                    NotificationType.SYSTEM,
                    "Lease Finalized: " + property.getTitle(),
                    "Congratulations! You have been confirmed as tenant for " + property.getTitle() + " starting on " + request.leaseStartDate(),
                    "RENTAL",
                    savedRecord.getId().toString()
            );
        }

        return RentalRecordResponse.fromEntity(savedRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public RentalRecordResponse getRentalRecordByProperty(UUID ownerId, UUID propertyId) {
        Property property = propertyRepository.findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", "id", propertyId));

        if (!property.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not have permission to view rental records for this property");
        }

        RentalRecord record = rentalRecordRepository.findFirstByPropertyIdOrderByCreatedAtDesc(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("RentalRecord", "propertyId", propertyId));

        return RentalRecordResponse.fromEntity(record);
    }
}

