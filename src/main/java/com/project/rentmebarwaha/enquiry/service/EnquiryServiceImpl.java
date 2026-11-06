package com.project.rentmebarwaha.enquiry.service;

import com.project.rentmebarwaha.common.exception.BadRequestException;
import com.project.rentmebarwaha.common.exception.ForbiddenException;
import com.project.rentmebarwaha.common.exception.RateLimitException;
import com.project.rentmebarwaha.common.exception.ResourceNotFoundException;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.enquiry.dto.CreateEnquiryRequest;
import com.project.rentmebarwaha.enquiry.dto.EnquiryResponse;
import com.project.rentmebarwaha.enquiry.dto.UpdateEnquiryStatusRequest;
import com.project.rentmebarwaha.enquiry.entity.Enquiry;
import com.project.rentmebarwaha.enquiry.entity.EnquiryStatus;
import com.project.rentmebarwaha.enquiry.repository.EnquiryRepository;
import com.project.rentmebarwaha.notification.entity.NotificationType;
import com.project.rentmebarwaha.notification.service.NotificationService;
import com.project.rentmebarwaha.property.entity.Property;
import com.project.rentmebarwaha.property.entity.PropertyStatus;
import com.project.rentmebarwaha.property.repository.PropertyRepository;
import com.project.rentmebarwaha.user.entity.User;
import com.project.rentmebarwaha.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnquiryServiceImpl implements EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public EnquiryResponse createEnquiry(UUID tenantId, UUID propertyId, CreateEnquiryRequest request) {
        Property property = propertyRepository.findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", "id", propertyId));

        if (property.getStatus() != PropertyStatus.ACTIVE) {
            throw new BadRequestException("Enquiries are only accepted for ACTIVE listings");
        }

        if (property.getOwner().getId().equals(tenantId)) {
            throw new BadRequestException("Property owners cannot submit enquiries for their own properties");
        }

        // 24-hour rate limiting check per property per tenant
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        boolean alreadyEnquired = enquiryRepository.existsByTenantIdAndPropertyIdAndCreatedAtAfter(tenantId, propertyId, cutoff);
        if (alreadyEnquired) {
            throw new RateLimitException("You have already sent an enquiry for this property within the last 24 hours.");
        }

        User tenant = userRepository.findByIdAndDeletedAtIsNull(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", tenantId));

        Enquiry enquiry = Enquiry.builder()
                .property(property)
                .tenant(tenant)
                .fullName(request.fullName().trim())
                .mobile(request.mobile().trim())
                .preferredMoveInDate(request.preferredMoveInDate())
                .tenantType(request.tenantType())
                .leaseDuration(request.leaseDuration())
                .visitTimings(request.visitTimings() != null ? new HashSet<>(request.visitTimings()) : new HashSet<>())
                .personalNote(request.personalNote())
                .status(EnquiryStatus.NEW)
                .build();

        Enquiry saved = enquiryRepository.save(enquiry);
        log.info("Created enquiry id: {} for property: {} by tenant: {}", saved.getId(), propertyId, tenantId);

        // Notify the property owner
        notificationService.sendNotification(
                property.getOwner().getId(),
                NotificationType.INQUIRY,
                "New Enquiry: " + property.getTitle(),
                request.fullName().trim() + " sent an enquiry with move-in date " + request.preferredMoveInDate(),
                "ENQUIRY",
                saved.getId().toString()
        );

        return EnquiryResponse.fromEntity(saved, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EnquiryResponse> getTenantEnquiries(UUID tenantId, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Enquiry> pageResult = enquiryRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);

        Page<EnquiryResponse> mappedPage = pageResult.map(e -> EnquiryResponse.fromEntity(e, true));
        return PageResponse.of(mappedPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EnquiryResponse> getOwnerEnquiries(UUID ownerId, UUID propertyId, EnquiryStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Enquiry> pageResult;
        if (propertyId != null && status != null) {
            pageResult = enquiryRepository.findByOwnerIdAndPropertyIdAndStatus(ownerId, propertyId, status, pageable);
        } else if (propertyId != null) {
            pageResult = enquiryRepository.findByOwnerIdAndPropertyId(ownerId, propertyId, pageable);
        } else if (status != null) {
            pageResult = enquiryRepository.findByOwnerIdAndStatus(ownerId, status, pageable);
        } else {
            pageResult = enquiryRepository.findByOwnerId(ownerId, pageable);
        }

        Page<EnquiryResponse> mappedPage = pageResult.map(e -> EnquiryResponse.fromEntity(e, false));
        return PageResponse.of(mappedPage);
    }

    @Override
    @Transactional(readOnly = true)
    public EnquiryResponse getEnquiryById(UUID userId, UUID enquiryId) {
        Enquiry enquiry = enquiryRepository.findByIdAndDeletedAtIsNull(enquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry", "id", enquiryId));

        boolean isTenant = enquiry.getTenant().getId().equals(userId);
        boolean isOwner = enquiry.getProperty().getOwner().getId().equals(userId);

        if (!isTenant && !isOwner) {
            throw new ForbiddenException("You do not have access to view this enquiry");
        }

        return EnquiryResponse.fromEntity(enquiry, isTenant);
    }

    @Override
    @Transactional
    public EnquiryResponse updateEnquiryStatus(UUID ownerId, UUID enquiryId, UpdateEnquiryStatusRequest request) {
        Enquiry enquiry = enquiryRepository.findByIdAndDeletedAtIsNull(enquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry", "id", enquiryId));

        if (!enquiry.getProperty().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not have permission to update enquiries for this property");
        }

        enquiry.setStatus(request.status());
        Enquiry saved = enquiryRepository.save(enquiry);
        log.info("Owner {} updated enquiry {} status to {}", ownerId, enquiryId, request.status());

        // Notify tenant about status change
        notificationService.sendNotification(
                enquiry.getTenant().getId(),
                NotificationType.INQUIRY,
                "Enquiry Update: " + enquiry.getProperty().getTitle(),
                "Your enquiry has been " + request.status().name().toLowerCase().replace("_", " "),
                "ENQUIRY",
                saved.getId().toString()
        );

        return EnquiryResponse.fromEntity(saved, false);
    }
}
