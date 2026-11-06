package com.project.rentmebarwaha.admin;

import com.project.rentmebarwaha.admin.dto.AdminPropertyReviewRequest;
import com.project.rentmebarwaha.admin.service.AdminServiceImpl;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.enquiry.repository.EnquiryRepository;
import com.project.rentmebarwaha.notification.entity.NotificationType;
import com.project.rentmebarwaha.notification.service.NotificationService;
import com.project.rentmebarwaha.property.dto.OwnerPropertySummaryResponse;
import com.project.rentmebarwaha.property.entity.Property;
import com.project.rentmebarwaha.property.entity.PropertyStatus;
import com.project.rentmebarwaha.property.repository.PropertyImageRepository;
import com.project.rentmebarwaha.property.repository.PropertyRepository;
import com.project.rentmebarwaha.user.dto.UserProfileResponse;
import com.project.rentmebarwaha.user.entity.Role;
import com.project.rentmebarwaha.user.entity.User;
import com.project.rentmebarwaha.user.repository.UserRepository;
import com.project.rentmebarwaha.visit.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyImageRepository propertyImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnquiryRepository enquiryRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User owner;
    private Property property;
    private UUID propertyId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        owner = User.builder()
                .id(ownerId)
                .name("Landlord Ram")
                .phone("9826012345")
                .role(Role.OWNER)
                .ownerVerified(false)
                .build();

        propertyId = UUID.randomUUID();
        property = Property.builder()
                .id(propertyId)
                .owner(owner)
                .title("2 BHK Flat near Temple")
                .status(PropertyStatus.PENDING_REVIEW)
                .build();
    }

    @Test
    void testApproveProperty_Success() {
        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(property));
        when(propertyRepository.save(any(Property.class))).thenAnswer(i -> i.getArgument(0));
        when(propertyImageRepository.findByPropertyIdOrderBySortOrderAsc(propertyId)).thenReturn(Collections.emptyList());

        OwnerPropertySummaryResponse response = adminService.approveProperty(propertyId);

        assertNotNull(response);
        assertEquals(PropertyStatus.ACTIVE, response.status());
        assertTrue(response.verified());
        verify(notificationService, times(1)).sendNotification(
                eq(ownerId), eq(NotificationType.SYSTEM), anyString(), anyString(), eq("PROPERTY"), eq(propertyId.toString())
        );
    }

    @Test
    void testRejectProperty_Success() {
        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(property));
        when(propertyRepository.save(any(Property.class))).thenAnswer(i -> i.getArgument(0));
        when(propertyImageRepository.findByPropertyIdOrderBySortOrderAsc(propertyId)).thenReturn(Collections.emptyList());

        AdminPropertyReviewRequest request = new AdminPropertyReviewRequest("Images are blurry");
        OwnerPropertySummaryResponse response = adminService.rejectProperty(propertyId, request);

        assertNotNull(response);
        assertEquals(PropertyStatus.DRAFT, response.status());
        verify(notificationService, times(1)).sendNotification(
                eq(ownerId), eq(NotificationType.SYSTEM), anyString(), contains("blurry"), eq("PROPERTY"), eq(propertyId.toString())
        );
    }

    @Test
    void testVerifyOwner_True() {
        when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(owner));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileResponse response = adminService.verifyOwner(ownerId, true);

        assertNotNull(response);
        assertTrue(response.ownerVerified());
        verify(notificationService, times(1)).sendNotification(
                eq(ownerId), eq(NotificationType.SYSTEM), anyString(), anyString(), eq("USER"), eq(ownerId.toString())
        );
    }

    @Test
    void testVerifyKyc_True() {
        when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(owner));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileResponse response = adminService.verifyKyc(ownerId, true);

        assertNotNull(response);
        assertTrue(response.kycVerified());
        verify(notificationService, times(1)).sendNotification(
                eq(ownerId), eq(NotificationType.SYSTEM), anyString(), anyString(), eq("USER"), eq(ownerId.toString())
        );
    }

    @Test
    void testGetPendingProperties() {
        when(propertyRepository.findByStatusAndDeletedAtIsNull(eq(PropertyStatus.PENDING_REVIEW), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(property)));
        when(propertyImageRepository.findByPropertyIdOrderBySortOrderAsc(propertyId)).thenReturn(Collections.emptyList());

        PageResponse<OwnerPropertySummaryResponse> page = adminService.getPendingProperties(0, 10);

        assertNotNull(page);
        assertEquals(1, page.content().size());
        assertEquals(PropertyStatus.PENDING_REVIEW, page.content().get(0).status());
    }
}

