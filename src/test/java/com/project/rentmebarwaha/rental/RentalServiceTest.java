package com.project.rentmebarwaha.rental;

import com.project.rentmebarwaha.common.exception.BadRequestException;
import com.project.rentmebarwaha.common.exception.ForbiddenException;
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
import com.project.rentmebarwaha.rental.service.RentalServiceImpl;
import com.project.rentmebarwaha.user.entity.Role;
import com.project.rentmebarwaha.user.entity.User;
import com.project.rentmebarwaha.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock
    private RentalRecordRepository rentalRecordRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnquiryRepository enquiryRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RentalServiceImpl rentalService;

    private User owner;
    private User tenant;
    private Property property;
    private UUID ownerId;
    private UUID tenantId;
    private UUID propertyId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        owner = User.builder()
                .id(ownerId)
                .name("Shyam Landlord")
                .role(Role.OWNER)
                .build();

        tenantId = UUID.randomUUID();
        tenant = User.builder()
                .id(tenantId)
                .name("Pooja Tenant")
                .role(Role.TENANT)
                .build();

        propertyId = UUID.randomUUID();
        property = Property.builder()
                .id(propertyId)
                .owner(owner)
                .title("3 BHK Riverview Bungalow")
                .status(PropertyStatus.ACTIVE)
                .build();
    }

    @Test
    void testMarkPropertyAsRented_ViaNestEarth_WithAutoArchive() {
        MarkRentedRequest request = new MarkRentedRequest(
                RentalSource.VIA_NESTEARTH,
                LocalDate.now().plusDays(1),
                tenantId,
                true
        );

        Enquiry e1 = Enquiry.builder().id(UUID.randomUUID()).status(EnquiryStatus.NEW).build();
        Enquiry e2 = Enquiry.builder().id(UUID.randomUUID()).status(EnquiryStatus.REPLIED).build();

        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(property));
        when(userRepository.findByIdAndDeletedAtIsNull(tenantId)).thenReturn(Optional.of(tenant));
        when(rentalRecordRepository.save(any(RentalRecord.class))).thenAnswer(i -> {
            RentalRecord r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(enquiryRepository.findByPropertyIdAndStatusIn(eq(propertyId), anyList())).thenReturn(List.of(e1, e2));

        RentalRecordResponse response = rentalService.markPropertyAsRented(ownerId, propertyId, request);

        assertNotNull(response);
        assertEquals(PropertyStatus.RENTED, property.getStatus());
        assertEquals(RentalSource.VIA_NESTEARTH, response.source());
        assertEquals(BigDecimal.ZERO, response.platformFeePercent()); // Zero brokerage
        assertEquals(EnquiryStatus.CLOSED, e1.getStatus());
        assertEquals(EnquiryStatus.CLOSED, e2.getStatus());

        verify(notificationService, times(1)).sendNotification(
                eq(tenantId), eq(NotificationType.SYSTEM), anyString(), anyString(), eq("RENTAL"), anyString()
        );
        verify(enquiryRepository, times(1)).saveAll(List.of(e1, e2));
    }

    @Test
    void testMarkPropertyAsRented_AlreadyRented_ThrowsBadRequest() {
        property.setStatus(PropertyStatus.RENTED);
        MarkRentedRequest request = new MarkRentedRequest(
                RentalSource.EXTERNAL,
                LocalDate.now(),
                null,
                false
        );

        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(property));

        assertThrows(BadRequestException.class, () -> rentalService.markPropertyAsRented(ownerId, propertyId, request));
    }

    @Test
    void testMarkPropertyAsRented_Forbidden_IfNotOwner() {
        UUID strangerId = UUID.randomUUID();
        MarkRentedRequest request = new MarkRentedRequest(
                RentalSource.FAMILY_FRIENDS,
                LocalDate.now(),
                null,
                false
        );

        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(property));

        assertThrows(ForbiddenException.class, () -> rentalService.markPropertyAsRented(strangerId, propertyId, request));
    }

    @Test
    void testGetRentalRecordByProperty_Success() {
        RentalRecord record = RentalRecord.builder()
                .id(UUID.randomUUID())
                .property(property)
                .tenant(tenant)
                .source(RentalSource.VIA_NESTEARTH)
                .leaseStartDate(LocalDate.now())
                .platformFeePercent(BigDecimal.ZERO)
                .build();

        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(property));
        when(rentalRecordRepository.findFirstByPropertyIdOrderByCreatedAtDesc(propertyId)).thenReturn(Optional.of(record));

        RentalRecordResponse response = rentalService.getRentalRecordByProperty(ownerId, propertyId);

        assertNotNull(response);
        assertEquals(record.getId(), response.id());
        assertEquals(propertyId, response.propertyId());
    }
}

