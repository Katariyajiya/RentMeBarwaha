package com.project.rentmebarwaha.agreement;

import com.project.rentmebarwaha.agreement.dto.AgreementResponse;
import com.project.rentmebarwaha.agreement.dto.CreateAgreementRequest;
import com.project.rentmebarwaha.agreement.entity.Agreement;
import com.project.rentmebarwaha.agreement.entity.AgreementStatus;
import com.project.rentmebarwaha.agreement.repository.AgreementRepository;
import com.project.rentmebarwaha.agreement.service.AgreementServiceImpl;
import com.project.rentmebarwaha.common.exception.BadRequestException;
import com.project.rentmebarwaha.common.exception.ForbiddenException;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.notification.service.NotificationService;
import com.project.rentmebarwaha.property.entity.Property;
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
class AgreementServiceTest {

    @Mock
    private AgreementRepository agreementRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AgreementServiceImpl agreementService;

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
                .name("Kishore Owner")
                .role(Role.OWNER)
                .build();

        tenantId = UUID.randomUUID();
        tenant = User.builder()
                .id(tenantId)
                .name("Anil Tenant")
                .role(Role.TENANT)
                .build();

        propertyId = UUID.randomUUID();
        property = Property.builder()
                .id(propertyId)
                .owner(owner)
                .title("2 BHK Flat Main City")
                .build();
    }

    @Test
    void testCreateAgreement_Success() {
        CreateAgreementRequest request = new CreateAgreementRequest(
                propertyId,
                tenantId,
                LocalDate.now(),
                LocalDate.now().plusMonths(11),
                BigDecimal.valueOf(8000),
                BigDecimal.valueOf(16000),
                "https://s3.amazonaws.com/agreements/sample.pdf"
        );

        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(property));
        when(userRepository.findByIdAndDeletedAtIsNull(tenantId)).thenReturn(Optional.of(tenant));
        when(agreementRepository.save(any(Agreement.class))).thenAnswer(i -> {
            Agreement a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        AgreementResponse response = agreementService.createAgreement(ownerId, request);

        assertNotNull(response);
        assertEquals(AgreementStatus.DRAFT, response.status());
        assertEquals(BigDecimal.valueOf(8000), response.rent());
        verify(notificationService, times(1)).sendNotification(
                eq(tenantId), any(), anyString(), anyString(), eq("AGREEMENT"), anyString()
        );
    }

    @Test
    void testCreateAgreement_InvalidDates_ThrowsBadRequest() {
        CreateAgreementRequest request = new CreateAgreementRequest(
                propertyId,
                tenantId,
                LocalDate.now().plusMonths(5),
                LocalDate.now(), // End date before start date
                BigDecimal.valueOf(8000),
                BigDecimal.valueOf(16000),
                null
        );

        assertThrows(BadRequestException.class, () -> agreementService.createAgreement(ownerId, request));
    }

    @Test
    void testCreateAgreement_Forbidden_IfNotPropertyOwner() {
        UUID strangerId = UUID.randomUUID();
        CreateAgreementRequest request = new CreateAgreementRequest(
                propertyId,
                tenantId,
                LocalDate.now(),
                LocalDate.now().plusMonths(11),
                BigDecimal.valueOf(8000),
                BigDecimal.valueOf(16000),
                null
        );

        when(propertyRepository.findByIdAndDeletedAtIsNull(propertyId)).thenReturn(Optional.of(property));

        assertThrows(ForbiddenException.class, () -> agreementService.createAgreement(strangerId, request));
    }

    @Test
    void testGetMyAgreements_Owner() {
        when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(owner));
        Agreement a = Agreement.builder()
                .id(UUID.randomUUID())
                .property(property)
                .owner(owner)
                .tenant(tenant)
                .status(AgreementStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(11))
                .rent(BigDecimal.valueOf(9000))
                .build();

        when(agreementRepository.findByOwnerIdOrderByCreatedAtDesc(eq(ownerId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a)));

        PageResponse<AgreementResponse> page = agreementService.getMyAgreements(ownerId, 0, 10);

        assertNotNull(page);
        assertEquals(1, page.content().size());
        assertEquals(AgreementStatus.ACTIVE, page.content().get(0).status());
    }

    @Test
    void testUpdateAgreementStatus_Success() {
        UUID agreementId = UUID.randomUUID();
        Agreement a = Agreement.builder()
                .id(agreementId)
                .property(property)
                .owner(owner)
                .tenant(tenant)
                .status(AgreementStatus.DRAFT)
                .build();

        when(agreementRepository.findById(agreementId)).thenReturn(Optional.of(a));
        when(agreementRepository.save(any(Agreement.class))).thenAnswer(i -> i.getArgument(0));

        AgreementResponse response = agreementService.updateAgreementStatus(tenantId, agreementId, AgreementStatus.ACTIVE);

        assertNotNull(response);
        assertEquals(AgreementStatus.ACTIVE, response.status());
        verify(notificationService, times(1)).sendNotification(
                eq(ownerId), any(), anyString(), anyString(), eq("AGREEMENT"), eq(agreementId.toString())
        );
    }
}

