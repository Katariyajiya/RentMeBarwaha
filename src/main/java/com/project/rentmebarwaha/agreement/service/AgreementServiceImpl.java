package com.project.rentmebarwaha.agreement.service;

import com.project.rentmebarwaha.agreement.dto.AgreementResponse;
import com.project.rentmebarwaha.agreement.dto.CreateAgreementRequest;
import com.project.rentmebarwaha.agreement.entity.Agreement;
import com.project.rentmebarwaha.agreement.entity.AgreementStatus;
import com.project.rentmebarwaha.agreement.repository.AgreementRepository;
import com.project.rentmebarwaha.common.exception.BadRequestException;
import com.project.rentmebarwaha.common.exception.ForbiddenException;
import com.project.rentmebarwaha.common.exception.ResourceNotFoundException;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.notification.entity.NotificationType;
import com.project.rentmebarwaha.notification.service.NotificationService;
import com.project.rentmebarwaha.property.entity.Property;
import com.project.rentmebarwaha.property.repository.PropertyRepository;
import com.project.rentmebarwaha.user.entity.Role;
import com.project.rentmebarwaha.user.entity.User;
import com.project.rentmebarwaha.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgreementServiceImpl implements AgreementService {

    private final AgreementRepository agreementRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public AgreementResponse createAgreement(UUID ownerId, CreateAgreementRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("Lease end date must be after lease start date");
        }

        Property property = propertyRepository.findByIdAndDeletedAtIsNull(request.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property", "id", request.propertyId()));

        if (!property.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You can only draft agreements for your own properties");
        }

        User tenant = userRepository.findByIdAndDeletedAtIsNull(request.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", request.tenantId()));

        User owner = property.getOwner();

        Agreement agreement = Agreement.builder()
                .property(property)
                .owner(owner)
                .tenant(tenant)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .rent(request.rent())
                .deposit(request.deposit() != null ? request.deposit() : BigDecimal.ZERO)
                .documentUrl(request.documentUrl())
                .status(AgreementStatus.DRAFT)
                .build();

        Agreement saved = agreementRepository.save(agreement);
        log.info("Agreement draft created id: {} for property: {} by owner: {}", saved.getId(), property.getId(), ownerId);

        // Notify tenant about new agreement draft
        notificationService.sendNotification(
                tenant.getId(),
                NotificationType.SYSTEM,
                "Rental Agreement Drafted: " + property.getTitle(),
                "Landlord " + owner.getName() + " drafted a rental agreement for your review.",
                "AGREEMENT",
                saved.getId().toString()
        );

        return AgreementResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AgreementResponse getAgreementById(UUID userId, UUID agreementId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", "id", agreementId));

        if (!agreement.getOwner().getId().equals(userId) && !agreement.getTenant().getId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to view this agreement");
        }

        return AgreementResponse.fromEntity(agreement);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AgreementResponse> getMyAgreements(UUID userId, int page, int size) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        Page<Agreement> agreementPage;

        if (user.getRole() == Role.OWNER) {
            agreementPage = agreementRepository.findByOwnerIdOrderByCreatedAtDesc(userId, pageable);
        } else {
            agreementPage = agreementRepository.findByTenantIdOrderByCreatedAtDesc(userId, pageable);
        }

        Page<AgreementResponse> mappedPage = agreementPage.map(AgreementResponse::fromEntity);
        return PageResponse.of(mappedPage);
    }

    @Override
    @Transactional
    public AgreementResponse updateAgreementStatus(UUID userId, UUID agreementId, AgreementStatus status) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", "id", agreementId));

        boolean isOwner = agreement.getOwner().getId().equals(userId);
        boolean isTenant = agreement.getTenant().getId().equals(userId);

        if (!isOwner && !isTenant) {
            throw new ForbiddenException("You do not have permission to update this agreement");
        }

        agreement.setStatus(status);
        Agreement saved = agreementRepository.save(agreement);
        log.info("User {} updated agreement {} status to {}", userId, agreementId, status);

        // Notify other party
        UUID targetUserId = isOwner ? agreement.getTenant().getId() : agreement.getOwner().getId();
        notificationService.sendNotification(
                targetUserId,
                NotificationType.SYSTEM,
                "Rental Agreement Update: " + agreement.getProperty().getTitle(),
                "Agreement status has been updated to " + status.name(),
                "AGREEMENT",
                saved.getId().toString()
        );

        return AgreementResponse.fromEntity(saved);
    }
}

