package com.project.rentmebarwaha.property.service;

import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.property.dto.PropertyDetailResponse;
import com.project.rentmebarwaha.property.dto.PropertyFilterCriteria;
import com.project.rentmebarwaha.property.dto.PropertyListItemResponse;

import java.util.List;
import java.util.UUID;

public interface PropertyBrowseService {

    PageResponse<PropertyListItemResponse> searchProperties(PropertyFilterCriteria criteria, UUID currentUserId);

    PropertyDetailResponse getPropertyDetail(UUID id, UUID currentUserId, String clientIdentifier);

    List<String> getLocalities(String city);
}

