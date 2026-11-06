package com.project.rentmebarwaha.enquiry;

import com.project.rentmebarwaha.common.exception.BadRequestException;
import com.project.rentmebarwaha.common.exception.ForbiddenException;
import com.project.rentmebarwaha.common.exception.RateLimitException;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.enquiry.dto.CreateEnquiryRequest;
import com.project.rentmebarwaha.enquiry.dto.EnquiryResponse;
import com.project.rentmebarwaha.enquiry.dto.UpdateEnquiryStatusRequest;
import com.project.rentmebarwaha.enquiry.entity.Enquiry;
import com.project.rentmebarwaha.enquiry.entity.EnquiryStatus;
import com.project.rentmebarwaha.enquiry.entity.LeaseDuration;
import com.project.rentmebarwaha.enquiry.entity.TenantType;
import com.project.rentmebarwaha.enquiry.entity.VisitTiming;
import com.project.rentmebarwaha.enquiry.repository.EnquiryRepository;
import com.project.rentmebarwaha.enquiry.service.EnquiryServiceImpl;
import com.project.rentmebarwaha.notification.entity.NotificationType;
import com.project.rentmebarwaha.notification.service.NotificationService;
import com.project.rentmebarwaha.property.entity.Property;
import com.project.rentmebarwaha.property.entity.PropertyStatus;
import com.project.rentmebarwaha.property.repository.PropertyRepository;
import com.project.rentmebarwaha.user.entity.Role;
import com.project.rentmebarwaha.user.entity.User;
import com.project.rentmebarwaha.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnquiryServiceTest {

    @Mock
    private EnquiryRepository enquiryRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EnquiryServiceImpl enquiryService;

    private User owner;
    private User tenant;
    private Property property;
    private UUID propertyId;
    private UUID tenantId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        owner = User.builder()
                .id(ownerId)
                .name("Ramesh Landlord")
                .phone("9826011111")
                .role(Role.OWNER)
                .build();

        tenantId = UUID.randomUUID();
        tenant = User.builder()
                .id(tenantId)
                .name("Suresh Tenant")
                .phone("9826022222")
                .role(Role.TENANT)
                .build();

        propertyId = UUID.randomUUID();
        property = Property.builder()
                .id(propertyId)
                .owner(owner)
                .title("2 BHK at Main Market")
                .status(PropertyStatus.ACTIVE)
                .build();
    }

    @Test
    void testCreateEnquiry_Success() {
        CreateEnquiryRequest request = new CreateEnquiryRequest(
                "Suresh Tenant",
                "9826022222",
                LocalDate.now().plusDays(10),
                TenantType.FAMILY,
                LeaseDuration.ELEVEN_MONTHS,
                Collections.singleton(VisitTiming.SUNDAY_ANYTIME),
                "Looking forward to seeing the house"
        );

        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(property));
        when(enquiryRepository.existsByTenantIdAndPropertyIdAndCreatedAtAfter(eq(tenantId), eq(propertyId), any(Instant.class)))
                .thenReturn(false);
        when(userRepository.findByIdAndDeletedAtIsNull(tenantId)).thenReturn(Optional.of(tenant));
        when(enquiryRepository.save(any(Enquiry.class))).thenAnswer(i -> {
            Enquiry e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        EnquiryResponse response = enquiryService.createEnquiry(tenantId, propertyId, request);

        assertNotNull(response);
        assertEquals("Suresh Tenant", response.fullName());
        assertEquals(EnquiryStatus.NEW, response.status());
        assertNull(response.ownerPhone()); // Hidden on initial creation
        verify(notificationService, times(1)).sendNotification(
                eq(ownerId), eq(NotificationType.INQUIRY), anyString(), anyString(), eq("ENQUIRY"), anyString()
        );
    }

    @Test
    void testCreateEnquiry_PropertyNotActive_ThrowsBadRequest() {
        property.setStatus(PropertyStatus.RENTED);
        CreateEnquiryRequest request = new CreateEnquiryRequest(
                "Suresh Tenant", "9826022222", LocalDate.now().plusDays(5),
                TenantType.FAMILY, LeaseDuration.ELEVEN_MONTHS,
                Collections.singleton(VisitTiming.SUNDAY_ANYTIME), null
        );

        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(property));

        assertThrows(BadRequestException.class, () -> enquiryService.createEnquiry(tenantId, propertyId, request));
    }

    @Test
    void testCreateEnquiry_OwnerEnquiringOwnProperty_ThrowsBadRequest() {
        CreateEnquiryRequest request = new CreateEnquiryRequest(
                "Ramesh Landlord", "9826011111", LocalDate.now().plusDays(5),
                TenantType.FAMILY, LeaseDuration.ELEVEN_MONTHS,
                Collections.singleton(VisitTiming.SUNDAY_ANYTIME), null
        );

        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(property));

        assertThrows(BadRequestException.class, () -> enquiryService.createEnquiry(ownerId, propertyId, request));
    }

    @Test
    void testCreateEnquiry_RateLimitExceeded_ThrowsRateLimitException() {
        CreateEnquiryRequest request = new CreateEnquiryRequest(
                "Suresh Tenant", "9826022222", LocalDate.now().plusDays(5),
                TenantType.FAMILY, LeaseDuration.ELEVEN_MONTHS,
                Collections.singleton(VisitTiming.SUNDAY_ANYTIME), null
        );

        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(property));
        when(enquiryRepository.existsByTenantIdAndPropertyIdAndCreatedAtAfter(eq(tenantId), eq(propertyId), any(Instant.class)))
                .thenReturn(true);

        assertThrows(RateLimitException.class, () -> enquiryService.createEnquiry(tenantId, propertyId, request));
    }

    @Test
    void testGetTenantEnquiries_RevealsPhoneOnlyWhenAccepted() {
        Enquiry e1 = Enquiry.builder()
                .id(UUID.randomUUID())
                .property(property)
                .tenant(tenant)
                .fullName("Suresh")
                .mobile("9826022222")
                .status(EnquiryStatus.ACCEPTED)
                .build();

        Enquiry e2 = Enquiry.builder()
                .id(UUID.randomUUID())
                .property(property)
                .tenant(tenant)
                .fullName("Suresh")
                .mobile("9826022222")
                .status(EnquiryStatus.NEW)
                .build();

        when(enquiryRepository.findByTenantIdAndDeletedAtIsNull(eq(tenantId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(e1, e2)));

        PageResponse<EnquiryResponse> response = enquiryService.getTenantEnquiries(tenantId, 0, 10);

        assertNotNull(response);
        assertEquals(2, response.content().size());
        assertEquals("9826011111", response.content().get(0).ownerPhone()); // ACCEPTED reveals phone
        assertNull(response.content().get(1).ownerPhone()); // NEW hides phone
    }

    @Test
    void testUpdateEnquiryStatus_Success() {
        UUID enquiryId = UUID.randomUUID();
        Enquiry enquiry = Enquiry.builder()
                .id(enquiryId)
                .property(property)
                .tenant(tenant)
                .fullName("Suresh")
                .mobile("9826022222")
                .status(EnquiryStatus.NEW)
                .build();

        when(enquiryRepository.findByIdAndDeletedAtIsNull(enquiryId)).thenReturn(Optional.of(enquiry));
        when(enquiryRepository.save(any(Enquiry.class))).thenAnswer(i -> i.getArgument(0));

        UpdateEnquiryStatusRequest update = new UpdateEnquiryStatusRequest(EnquiryStatus.ACCEPTED);
        EnquiryResponse response = enquiryService.updateEnquiryStatus(ownerId, enquiryId, update);

        assertNotNull(response);
        assertEquals(EnquiryStatus.ACCEPTED, response.status());
        verify(notificationService, times(1)).sendNotification(
                eq(tenantId), eq(NotificationType.INQUIRY), anyString(), anyString(), eq("ENQUIRY"), eq(enquiryId.toString())
        );
    }

    @Test
    void testUpdateEnquiryStatus_Forbidden_IfNotOwner() {
        UUID enquiryId = UUID.randomUUID();
        Enquiry enquiry = Enquiry.builder()
                .id(enquiryId)
                .property(property)
                .tenant(tenant)
                .status(EnquiryStatus.NEW)
                .build();

        when(enquiryRepository.findByIdAndDeletedAtIsNull(enquiryId)).thenReturn(Optional.of(enquiry));

        UpdateEnquiryStatusRequest update = new UpdateEnquiryStatusRequest(EnquiryStatus.REJECTED);
        UUID strangerId = UUID.randomUUID();

        assertThrows(ForbiddenException.class, () -> enquiryService.updateEnquiryStatus(strangerId, enquiryId, update));
    }
}
