package com.project.rentmebarwaha.saved.service;

import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.property.dto.PropertyListItemResponse;

import java.util.UUID;

public interface SavedPropertyService {

    void saveProperty(UUID userId, UUID propertyId);

    void unsaveProperty(UUID userId, UUID propertyId);

    PageResponse<PropertyListItemResponse> getSavedProperties(UUID userId, int page, int size);
}

