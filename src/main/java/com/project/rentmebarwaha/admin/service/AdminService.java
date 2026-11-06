package com.project.rentmebarwaha.admin.service;

import com.project.rentmebarwaha.admin.dto.AdminPropertyReviewRequest;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.property.dto.OwnerPropertySummaryResponse;
import com.project.rentmebarwaha.user.dto.UserProfileResponse;
import com.project.rentmebarwaha.user.entity.Role;

import java.util.UUID;

public interface AdminService {

    PageResponse<OwnerPropertySummaryResponse> getPendingProperties(int page, int size);

    OwnerPropertySummaryResponse approveProperty(UUID propertyId);

    OwnerPropertySummaryResponse rejectProperty(UUID propertyId, AdminPropertyReviewRequest request);

    UserProfileResponse verifyOwner(UUID userId, boolean verified);

    UserProfileResponse verifyKyc(UUID userId, boolean verified);

    PageResponse<UserProfileResponse> getUsers(Role role, int page, int size);
}

