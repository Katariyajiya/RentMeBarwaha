package com.project.rentmebarwaha.admin.service;

import com.project.rentmebarwaha.admin.dto.AdminPropertyReviewRequest;
import com.project.rentmebarwaha.common.exception.ResourceNotFoundException;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.enquiry.repository.EnquiryRepository;
import com.project.rentmebarwaha.notification.entity.NotificationType;
import com.project.rentmebarwaha.notification.service.NotificationService;
import com.project.rentmebarwaha.property.dto.OwnerPropertySummaryResponse;
import com.project.rentmebarwaha.property.dto.PropertyImageDto;
import com.project.rentmebarwaha.property.entity.Property;
import com.project.rentmebarwaha.property.entity.PropertyImage;
import com.project.rentmebarwaha.property.entity.PropertyStatus;
import com.project.rentmebarwaha.property.repository.PropertyImageRepository;
import com.project.rentmebarwaha.property.repository.PropertyRepository;
import com.project.rentmebarwaha.user.dto.UserProfileResponse;
import com.project.rentmebarwaha.user.entity.Role;
import com.project.rentmebarwaha.user.entity.User;
import com.project.rentmebarwaha.user.repository.UserRepository;
import com.project.rentmebarwaha.visit.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final UserRepository userRepository;
    private final EnquiryRepository enquiryRepository;
    private final VisitRepository visitRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OwnerPropertySummaryResponse> getPendingProperties(int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Property> pendingPage = propertyRepository.findByStatusAndDeletedAtIsNull(PropertyStatus.PENDING_REVIEW, pageable);

        Page<OwnerPropertySummaryResponse> mapped = pendingPage.map(this::mapToSummary);
        return PageResponse.of(mapped);
    }

    @Override
    @Transactional
    public OwnerPropertySummaryResponse approveProperty(UUID propertyId) {
        Property property = propertyRepository.findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", "id", propertyId));

        property.setStatus(PropertyStatus.ACTIVE);
        property.setVerified(true);
        if (property.getPublishedAt() == null) {
            property.setPublishedAt(Instant.now());
        }

        Property saved = propertyRepository.save(property);
        log.info("Admin approved property id: {}", propertyId);

        notificationService.sendNotification(
                property.getOwner().getId(),
                NotificationType.SYSTEM,
                "Listing Approved: " + property.getTitle(),
                "Your property listing has been approved and is now live on NestEarth.",
                "PROPERTY",
                saved.getId().toString()
        );

        return mapToSummary(saved);
    }

    @Override
    @Transactional
    public OwnerPropertySummaryResponse rejectProperty(UUID propertyId, AdminPropertyReviewRequest request) {
        Property property = propertyRepository.findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", "id", propertyId));

        property.setStatus(PropertyStatus.DRAFT);
        Property saved = propertyRepository.save(property);
        log.info("Admin rejected property id: {}", propertyId);

        String reason = (request != null && request.reason() != null && !request.reason().isBlank())
                ? request.reason().trim()
                : "Please ensure all photos and listing details follow our community guidelines.";

        notificationService.sendNotification(
                property.getOwner().getId(),
                NotificationType.SYSTEM,
                "Listing Requires Changes: " + property.getTitle(),
                "Your listing could not be approved: " + reason,
                "PROPERTY",
                saved.getId().toString()
        );

        return mapToSummary(saved);
    }

    @Override
    @Transactional
    public UserProfileResponse verifyOwner(UUID userId, boolean verified) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setOwnerVerified(verified);
        User saved = userRepository.save(user);
        log.info("Admin set ownerVerified={} for user: {}", verified, userId);

        if (verified) {
            notificationService.sendNotification(
                    userId,
                    NotificationType.SYSTEM,
                    "Verified Landlord Badge Granted",
                    "Congratulations! Your owner profile is now verified. Your future listings will publish instantly.",
                    "USER",
                    userId.toString()
            );
        }

        return UserProfileResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public UserProfileResponse verifyKyc(UUID userId, boolean verified) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setKycVerified(verified);
        User saved = userRepository.save(user);
        log.info("Admin set kycVerified={} for user: {}", verified, userId);

        if (verified) {
            notificationService.sendNotification(
                    userId,
                    NotificationType.SYSTEM,
                    "KYC Verification Completed",
                    "Your identity verification (KYC) has been successfully verified.",
                    "USER",
                    userId.toString()
            );
        }

        return UserProfileResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> getUsers(Role role, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        Page<User> userPage;
        if (role != null) {
            userPage = userRepository.findByRoleAndDeletedAtIsNull(role, pageable);
        } else {
            userPage = userRepository.findByDeletedAtIsNull(pageable);
        }

        return PageResponse.of(userPage.map(UserProfileResponse::fromEntity));
    }

    private OwnerPropertySummaryResponse mapToSummary(Property p) {
        List<PropertyImage> images = propertyImageRepository.findByPropertyIdOrderBySortOrderAsc(p.getId());
        String coverUrl = images.stream().filter(PropertyImage::isCover).map(PropertyImage::getUrl).findFirst()
                .orElse(!images.isEmpty() ? images.get(0).getUrl() : null);

        List<PropertyImageDto> imageDtos = images.stream().map(PropertyImageDto::fromEntity).toList();
        long visitCount = p.getId() != null ? visitRepository.countByPropertyId(p.getId()) : 0;
        long enquiryCount = p.getId() != null ? enquiryRepository.countByPropertyIdAndDeletedAtIsNull(p.getId()) : 0;

        return new OwnerPropertySummaryResponse(
                p.getId(),
                p.getTitle(),
                p.getConfig(),
                p.getLocality(),
                p.getCity(),
                p.getAddress(),
                p.getMonthlyRent(),
                p.getSecurityDeposit(),
                p.getFurnishing(),
                p.getStatus(),
                p.isVerified(),
                p.getViewCount(),
                visitCount,
                enquiryCount,
                coverUrl,
                imageDtos,
                p.getAmenities(),
                p.getTags(),
                p.getPublishedAt(),
                p.getCreatedAt()
        );
    }
}

