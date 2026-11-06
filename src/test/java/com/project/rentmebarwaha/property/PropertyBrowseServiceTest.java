package com.project.rentmebarwaha.property;

import com.project.rentmebarwaha.property.dto.PropertyDetailResponse;
import com.project.rentmebarwaha.property.entity.Furnishing;
import com.project.rentmebarwaha.property.entity.Property;
import com.project.rentmebarwaha.property.entity.PropertyConfig;
import com.project.rentmebarwaha.property.entity.PropertyStatus;
import com.project.rentmebarwaha.property.repository.PropertyImageRepository;
import com.project.rentmebarwaha.property.repository.PropertyRepository;
import com.project.rentmebarwaha.property.repository.PropertyViewRepository;
import com.project.rentmebarwaha.property.service.PropertyBrowseServiceImpl;
import com.project.rentmebarwaha.saved.repository.SavedPropertyRepository;
import com.project.rentmebarwaha.user.entity.Role;
import com.project.rentmebarwaha.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyBrowseServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyImageRepository propertyImageRepository;

    @Mock
    private PropertyViewRepository propertyViewRepository;

    @Mock
    private SavedPropertyRepository savedPropertyRepository;

    @InjectMocks
    private PropertyBrowseServiceImpl propertyBrowseService;

    private Property property;
    private UUID propertyId;
    private User owner;

    @BeforeEach
    void setUp() {
        propertyId = UUID.randomUUID();
        owner = User.builder()
                .id(UUID.randomUUID())
                .name("Anil Verma")
                .phone("9876543210") // Phone should never be exposed in public detail
                .role(Role.OWNER)
                .ownerVerified(true)
                .build();

        property = Property.builder()
                .id(propertyId)
                .owner(owner)
                .title("1 BHK Affordable House")
                .config(PropertyConfig.ONE_BHK)
                .locality("Station Road")
                .city("Barwaha")
                .address("Near Railway Station")
                .monthlyRent(BigDecimal.valueOf(6500))
                .securityDeposit(BigDecimal.valueOf(13000))
                .furnishing(Furnishing.UNFURNISHED)
                .status(PropertyStatus.ACTIVE)
                .viewCount(10)
                .build();
    }

    @Test
    void testGetPropertyDetail_DeduplicatesViewsWithin24Hours() {
        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(property));
        when(propertyImageRepository.findByPropertyIdOrderBySortOrderAsc(propertyId)).thenReturn(Collections.emptyList());
        when(savedPropertyRepository.existsByUserIdAndPropertyId(any(), eq(propertyId))).thenReturn(false);

        // Already viewed in the last 24 hours
        when(propertyViewRepository.existsByPropertyIdAndViewerIdentifierAndViewedAtAfter(eq(propertyId), anyString(), any(Instant.class)))
                .thenReturn(true);

        PropertyDetailResponse detail = propertyBrowseService.getPropertyDetail(propertyId, UUID.randomUUID(), "192.168.1.1");

        assertNotNull(detail);
        assertEquals(10, detail.viewCount()); // Not incremented
        verify(propertyViewRepository, never()).save(any());
        verify(propertyRepository, never()).save(property);
    }

    @Test
    void testGetPropertyDetail_IncrementsView_WhenNotViewedIn24Hours() {
        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(property));
        when(propertyImageRepository.findByPropertyIdOrderBySortOrderAsc(propertyId)).thenReturn(Collections.emptyList());
        when(savedPropertyRepository.existsByUserIdAndPropertyId(any(), eq(propertyId))).thenReturn(false);

        // Not viewed yet
        when(propertyViewRepository.existsByPropertyIdAndViewerIdentifierAndViewedAtAfter(eq(propertyId), anyString(), any(Instant.class)))
                .thenReturn(false);

        PropertyDetailResponse detail = propertyBrowseService.getPropertyDetail(propertyId, UUID.randomUUID(), "192.168.1.1");

        assertNotNull(detail);
        assertEquals(11, detail.viewCount()); // Incremented
        verify(propertyViewRepository).save(any());
        verify(propertyRepository).save(property);
    }

    @Test
    void testGetPropertyDetail_ZeroHarassment_HidesOwnerPhone() {
        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(property));
        when(propertyImageRepository.findByPropertyIdOrderBySortOrderAsc(propertyId)).thenReturn(Collections.emptyList());

        PropertyDetailResponse detail = propertyBrowseService.getPropertyDetail(propertyId, null, "127.0.0.1");

        assertNotNull(detail);
        assertNotNull(detail.ownerCard());
        assertEquals("Anil Verma", detail.ownerCard().name());
        assertTrue(detail.ownerCard().ownerVerified());
        // Verify owner phone is not present on OwnerCardDto
    }
}

