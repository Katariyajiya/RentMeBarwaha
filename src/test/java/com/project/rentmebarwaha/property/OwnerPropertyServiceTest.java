package com.project.rentmebarwaha.property;

import com.project.rentmebarwaha.common.exception.BadRequestException;
import com.project.rentmebarwaha.common.exception.ForbiddenException;
import com.project.rentmebarwaha.property.dto.*;
import com.project.rentmebarwaha.property.entity.*;
import com.project.rentmebarwaha.property.repository.PropertyImageRepository;
import com.project.rentmebarwaha.property.repository.PropertyRepository;
import com.project.rentmebarwaha.property.service.OwnerPropertyServiceImpl;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnerPropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyImageRepository propertyImageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.project.rentmebarwaha.enquiry.repository.EnquiryRepository enquiryRepository;

    @Mock
    private com.project.rentmebarwaha.visit.repository.VisitRepository visitRepository;

    @InjectMocks
    private OwnerPropertyServiceImpl ownerPropertyService;

    private User owner;
    private UUID ownerId;
    private Property sampleProperty;
    private UUID propertyId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        owner = User.builder()
                .id(ownerId)
                .name("Kailash Sharma")
                .role(Role.OWNER)
                .ownerVerified(false)
                .build();

        propertyId = UUID.randomUUID();
        sampleProperty = Property.builder()
                .id(propertyId)
                .owner(owner)
                .title("2 BHK Flat near Narmada Ghat")
                .config(PropertyConfig.TWO_BHK)
                .locality("Narmada Road")
                .city("Barwaha")
                .address("Near Old Bridge, Narmada Road")
                .monthlyRent(BigDecimal.valueOf(12000))
                .securityDeposit(BigDecimal.valueOf(24000))
                .furnishing(Furnishing.SEMI_FURNISHED)
                .status(PropertyStatus.DRAFT)
                .build();
    }

    @Test
    void testCreateDraftProperty_Success() {
        CreatePropertyRequest request = new CreatePropertyRequest(
                "2 BHK Flat near Narmada Ghat",
                "Spacious flat with cross ventilation",
                PropertyConfig.TWO_BHK,
                "Narmada Road",
                "Barwaha",
                "Near Old Bridge, Narmada Road",
                null, null, 950, 2,
                BigDecimal.valueOf(12000),
                BigDecimal.valueOf(24000),
                Furnishing.SEMI_FURNISHED,
                PreferredTenants.FAMILY,
                null, true, true,
                Collections.singleton("Parking"),
                Collections.singleton("Garden Facing")
        );

        when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(owner));
        when(propertyRepository.save(any(Property.class))).thenAnswer(i -> i.getArgument(0));

        OwnerPropertySummaryResponse response = ownerPropertyService.createDraftProperty(ownerId, request);

        assertNotNull(response);
        assertEquals("2 BHK Flat near Narmada Ghat", response.title());
        assertEquals(PropertyStatus.DRAFT, response.status());
    }

    @Test
    void testUpdateProperty_OwnershipForbidden() {
        UUID otherUserId = UUID.randomUUID();
        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(sampleProperty));

        UpdatePropertyRequest update = new UpdatePropertyRequest(
                "New Title", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        assertThrows(ForbiddenException.class, () -> ownerPropertyService.updateProperty(otherUserId, propertyId, update));
    }

    @Test
    void testPublishProperty_FewerThan3Images_ThrowsBadRequest() {
        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(sampleProperty));
        when(propertyImageRepository.countByPropertyId(propertyId)).thenReturn(2L); // Only 2 images

        assertThrows(BadRequestException.class, () -> ownerPropertyService.publishProperty(ownerId, propertyId));
    }

    @Test
    void testPublishProperty_UnverifiedOwner_PendingReview() {
        owner.setOwnerVerified(false);
        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(sampleProperty));
        when(propertyImageRepository.countByPropertyId(propertyId)).thenReturn(4L);
        when(propertyRepository.save(any(Property.class))).thenAnswer(i -> i.getArgument(0));

        OwnerPropertySummaryResponse response = ownerPropertyService.publishProperty(ownerId, propertyId);

        assertNotNull(response);
        assertEquals(PropertyStatus.PENDING_REVIEW, response.status());
        assertFalse(response.verified());
    }

    @Test
    void testPublishProperty_VerifiedOwner_AutoActive() {
        owner.setOwnerVerified(true);
        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(sampleProperty));
        when(propertyImageRepository.countByPropertyId(propertyId)).thenReturn(3L);
        when(propertyRepository.save(any(Property.class))).thenAnswer(i -> i.getArgument(0));

        OwnerPropertySummaryResponse response = ownerPropertyService.publishProperty(ownerId, propertyId);

        assertNotNull(response);
        assertEquals(PropertyStatus.ACTIVE, response.status());
        assertTrue(response.verified());
    }

    @Test
    void testGetOwnerProperties_IncludesVisitAndEnquiryCounts() {
        when(propertyRepository.findByOwnerIdAndStatusAndDeletedAtIsNull(eq(ownerId), eq(PropertyStatus.ACTIVE), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(sampleProperty)));
        when(visitRepository.countByPropertyId(propertyId)).thenReturn(8L);
        when(enquiryRepository.countByPropertyIdAndDeletedAtIsNull(propertyId)).thenReturn(14L);

        com.project.rentmebarwaha.common.model.PageResponse<OwnerPropertySummaryResponse> page =
                ownerPropertyService.getOwnerProperties(ownerId, PropertyStatus.ACTIVE, 0, 10);

        assertNotNull(page);
        assertEquals(1, page.content().size());
        assertEquals(8L, page.content().get(0).visitCount());
        assertEquals(14L, page.content().get(0).enquiryCount());
    }
}
