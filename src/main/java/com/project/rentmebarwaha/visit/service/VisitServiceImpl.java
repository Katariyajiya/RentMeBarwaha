package com.project.rentmebarwaha.visit.service;

import com.project.rentmebarwaha.common.exception.ForbiddenException;
import com.project.rentmebarwaha.common.exception.ResourceNotFoundException;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.enquiry.entity.Enquiry;
import com.project.rentmebarwaha.enquiry.entity.EnquiryStatus;
import com.project.rentmebarwaha.enquiry.repository.EnquiryRepository;
import com.project.rentmebarwaha.notification.entity.NotificationType;
import com.project.rentmebarwaha.notification.service.NotificationService;
import com.project.rentmebarwaha.property.entity.Property;
import com.project.rentmebarwaha.user.entity.Role;
import com.project.rentmebarwaha.user.entity.User;
import com.project.rentmebarwaha.user.repository.UserRepository;
import com.project.rentmebarwaha.visit.dto.ScheduleVisitRequest;
import com.project.rentmebarwaha.visit.dto.UpdateVisitStatusRequest;
import com.project.rentmebarwaha.visit.dto.VisitResponse;
import com.project.rentmebarwaha.visit.entity.Visit;
import com.project.rentmebarwaha.visit.entity.VisitStatus;
import com.project.rentmebarwaha.visit.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitServiceImpl implements VisitService {

    private final VisitRepository visitRepository;
    private final EnquiryRepository enquiryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public VisitResponse scheduleVisit(UUID userId, UUID enquiryId, ScheduleVisitRequest request) {
        Enquiry enquiry = enquiryRepository.findByIdAndDeletedAtIsNull(enquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry", "id", enquiryId));

        Property property = enquiry.getProperty();
        User tenant = enquiry.getTenant();
        User owner = property.getOwner();

        boolean isTenant = tenant.getId().equals(userId);
        boolean isOwner = owner.getId().equals(userId);

        if (!isTenant && !isOwner) {
            throw new ForbiddenException("You do not have permission to schedule a visit for this enquiry");
        }

        VisitStatus initialStatus = isOwner ? VisitStatus.CONFIRMED : VisitStatus.REQUESTED;

        Visit visit = Visit.builder()
                .enquiry(enquiry)
                .property(property)
                .tenant(tenant)
                .owner(owner)
                .scheduledAt(request.scheduledAt())
                .status(initialStatus)
                .notes(request.notes())
                .build();

        Visit saved = visitRepository.save(visit);
        log.info("Visit scheduled id: {} by user: {} for property: {}", saved.getId(), userId, property.getId());

        // Update enquiry status if it was NEW or REPLIED
        if (enquiry.getStatus() == EnquiryStatus.NEW || enquiry.getStatus() == EnquiryStatus.REPLIED) {
            enquiry.setStatus(EnquiryStatus.VISIT_SCHEDULED);
            enquiryRepository.save(enquiry);
        }

        // Notify counter-party
        if (isTenant) {
            notificationService.sendNotification(
                    owner.getId(),
                    NotificationType.VISIT,
                    "Site Visit Requested: " + property.getTitle(),
                    tenant.getName() + " requested a visit on " + request.scheduledAt(),
                    "VISIT",
                    saved.getId().toString()
            );
        } else {
            notificationService.sendNotification(
                    tenant.getId(),
                    NotificationType.VISIT,
                    "Site Visit Scheduled: " + property.getTitle(),
                    "The landlord confirmed a site visit on " + request.scheduledAt(),
                    "VISIT",
                    saved.getId().toString()
            );
        }

        return VisitResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VisitResponse> getMyVisits(UUID userId, int page, int size) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        Page<Visit> visitPage;

        if (user.getRole() == Role.OWNER) {
            visitPage = visitRepository.findByOwnerIdOrderByScheduledAtAsc(userId, pageable);
        } else {
            visitPage = visitRepository.findByTenantIdOrderByScheduledAtAsc(userId, pageable);
        }

        Page<VisitResponse> mappedPage = visitPage.map(VisitResponse::fromEntity);
        return PageResponse.of(mappedPage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitResponse> getUpcomingVisitsForOwner(UUID ownerId, int limit) {
        PageRequest pageable = PageRequest.of(0, Math.min(Math.max(1, limit), 20));
        List<Visit> visits = visitRepository.findUpcomingVisitsByOwner(ownerId, Instant.now(), pageable);
        return visits.stream().map(VisitResponse::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VisitResponse getVisitById(UUID userId, UUID visitId) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit", "id", visitId));

        if (!visit.getTenant().getId().equals(userId) && !visit.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("You do not have access to view this visit");
        }

        return VisitResponse.fromEntity(visit);
    }

    @Override
    @Transactional
    public VisitResponse updateVisitStatus(UUID userId, UUID visitId, UpdateVisitStatusRequest request) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit", "id", visitId));

        boolean isTenant = visit.getTenant().getId().equals(userId);
        boolean isOwner = visit.getOwner().getId().equals(userId);

        if (!isTenant && !isOwner) {
            throw new ForbiddenException("You do not have permission to update this visit");
        }

        visit.setStatus(request.status());
        if (request.notes() != null && !request.notes().trim().isEmpty()) {
            visit.setNotes(request.notes().trim());
        }

        Visit saved = visitRepository.save(visit);
        log.info("User {} updated visit {} status to {}", userId, visitId, request.status());

        // Notify the other party
        UUID notifyTarget = isTenant ? visit.getOwner().getId() : visit.getTenant().getId();
        notificationService.sendNotification(
                notifyTarget,
                NotificationType.VISIT,
                "Visit Status: " + visit.getProperty().getTitle(),
                "The scheduled visit is now " + request.status().name().toLowerCase(),
                "VISIT",
                saved.getId().toString()
        );

        return VisitResponse.fromEntity(saved);
    }
}

