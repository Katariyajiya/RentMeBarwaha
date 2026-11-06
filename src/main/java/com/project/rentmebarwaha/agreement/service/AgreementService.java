package com.project.rentmebarwaha.agreement.service;

import com.project.rentmebarwaha.agreement.dto.AgreementResponse;
import com.project.rentmebarwaha.agreement.dto.CreateAgreementRequest;
import com.project.rentmebarwaha.agreement.entity.AgreementStatus;
import com.project.rentmebarwaha.common.model.PageResponse;

import java.util.UUID;

public interface AgreementService {

    AgreementResponse createAgreement(UUID ownerId, CreateAgreementRequest request);

    AgreementResponse getAgreementById(UUID userId, UUID agreementId);

    PageResponse<AgreementResponse> getMyAgreements(UUID userId, int page, int size);

    AgreementResponse updateAgreementStatus(UUID userId, UUID agreementId, AgreementStatus status);
}

