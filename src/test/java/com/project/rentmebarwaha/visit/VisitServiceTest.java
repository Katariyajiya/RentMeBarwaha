package com.project.rentmebarwaha.visit;

import com.project.rentmebarwaha.common.exception.ForbiddenException;
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
import com.project.rentmebarwaha.visit.service.VisitServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitServiceTest {

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private EnquiryRepository enquiryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private VisitServiceImpl visitService;

    private User owner;
    private User tenant;
    private Property property;
    private Enquiry enquiry;
    private UUID ownerId;
    private UUID tenantId;
    private UUID enquiryId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        owner = User.builder()
                .id(ownerId)
                .name("Kailash Owner")
                .role(Role.OWNER)
                .build();

        tenantId = UUID.randomUUID();
        tenant = User.builder()
                .id(tenantId)
                .name("Vijay Tenant")
                .role(Role.TENANT)
                .build();

        property = Property.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .title("Cozy Independent House")
                .locality("Mortakka Road")
                .build();

        enquiryId = UUID.randomUUID();
        enquiry = Enquiry.builder()
                .id(enquiryId)
                .property(property)
                .tenant(tenant)
                .status(EnquiryStatus.NEW)
                .build();
    }

    @Test
    void testScheduleVisit_ByTenant_Success() {
        Instant visitTime = Instant.now().plus(2, ChronoUnit.DAYS);
        ScheduleVisitRequest request = new ScheduleVisitRequest(visitTime, "Please confirm if Sunday works");

        when(enquiryRepository.findByIdAndDeletedAtIsNull(enquiryId)).thenReturn(Optional.of(enquiry));
        when(visitRepository.save(any(Visit.class))).thenAnswer(i -> {
            Visit v = i.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        VisitResponse response = visitService.scheduleVisit(tenantId, enquiryId, request);

        assertNotNull(response);
        assertEquals(VisitStatus.REQUESTED, response.status());
        assertEquals(EnquiryStatus.VISIT_SCHEDULED, enquiry.getStatus());
        verify(notificationService, times(1)).sendNotification(
                eq(ownerId), eq(NotificationType.VISIT), anyString(), anyString(), eq("VISIT"), anyString()
        );
    }

    @Test
    void testScheduleVisit_ByOwner_Success() {
        Instant visitTime = Instant.now().plus(1, ChronoUnit.DAYS);
        ScheduleVisitRequest request = new ScheduleVisitRequest(visitTime, "I will be available at 4 PM");

        when(enquiryRepository.findByIdAndDeletedAtIsNull(enquiryId)).thenReturn(Optional.of(enquiry));
        when(visitRepository.save(any(Visit.class))).thenAnswer(i -> {
            Visit v = i.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        VisitResponse response = visitService.scheduleVisit(ownerId, enquiryId, request);

        assertNotNull(response);
        assertEquals(VisitStatus.CONFIRMED, response.status());
        verify(notificationService, times(1)).sendNotification(
                eq(tenantId), eq(NotificationType.VISIT), anyString(), anyString(), eq("VISIT"), anyString()
        );
    }

    @Test
    void testScheduleVisit_Forbidden_IfNotParty() {
        UUID strangerId = UUID.randomUUID();
        ScheduleVisitRequest request = new ScheduleVisitRequest(Instant.now().plus(1, ChronoUnit.DAYS), "Hi");
        when(enquiryRepository.findByIdAndDeletedAtIsNull(enquiryId)).thenReturn(Optional.of(enquiry));

        assertThrows(ForbiddenException.class, () -> visitService.scheduleVisit(strangerId, enquiryId, request));
    }

    @Test
    void testGetMyVisits_Tenant() {
        when(userRepository.findByIdAndDeletedAtIsNull(tenantId)).thenReturn(Optional.of(tenant));
        Visit v = Visit.builder()
                .id(UUID.randomUUID())
                .enquiry(enquiry)
                .property(property)
                .tenant(tenant)
                .owner(owner)
                .scheduledAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .status(VisitStatus.CONFIRMED)
                .build();

        when(visitRepository.findByTenantIdOrderByScheduledAtAsc(eq(tenantId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(v)));

        PageResponse<VisitResponse> response = visitService.getMyVisits(tenantId, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertEquals(VisitStatus.CONFIRMED, response.content().get(0).status());
    }

    @Test
    void testUpdateVisitStatus_Success() {
        UUID visitId = UUID.randomUUID();
        Visit v = Visit.builder()
                .id(visitId)
                .enquiry(enquiry)
                .property(property)
                .tenant(tenant)
                .owner(owner)
                .scheduledAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .status(VisitStatus.REQUESTED)
                .build();

        when(visitRepository.findById(visitId)).thenReturn(Optional.of(v));
        when(visitRepository.save(any(Visit.class))).thenAnswer(i -> i.getArgument(0));

        UpdateVisitStatusRequest request = new UpdateVisitStatusRequest(VisitStatus.CONFIRMED, "Confirmed time works");
        VisitResponse response = visitService.updateVisitStatus(ownerId, visitId, request);

        assertNotNull(response);
        assertEquals(VisitStatus.CONFIRMED, response.status());
        assertEquals("Confirmed time works", response.notes());
        verify(notificationService, times(1)).sendNotification(
                eq(tenantId), eq(NotificationType.VISIT), anyString(), anyString(), eq("VISIT"), eq(visitId.toString())
        );
    }
}

