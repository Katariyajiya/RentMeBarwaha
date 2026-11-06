package com.project.rentmebarwaha.enquiry.service;

import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.enquiry.dto.CreateEnquiryRequest;
import com.project.rentmebarwaha.enquiry.dto.EnquiryResponse;
import com.project.rentmebarwaha.enquiry.dto.UpdateEnquiryStatusRequest;
import com.project.rentmebarwaha.enquiry.entity.EnquiryStatus;

import java.util.UUID;

public interface EnquiryService {

    EnquiryResponse createEnquiry(UUID tenantId, UUID propertyId, CreateEnquiryRequest request);

    PageResponse<EnquiryResponse> getTenantEnquiries(UUID tenantId, int page, int size);

    PageResponse<EnquiryResponse> getOwnerEnquiries(UUID ownerId, UUID propertyId, EnquiryStatus status, int page, int size);

    EnquiryResponse getEnquiryById(UUID userId, UUID enquiryId);

    EnquiryResponse updateEnquiryStatus(UUID ownerId, UUID enquiryId, UpdateEnquiryStatusRequest request);
}

