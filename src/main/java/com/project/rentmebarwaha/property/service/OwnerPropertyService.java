package com.project.rentmebarwaha.property.service;

import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.property.dto.*;
import com.project.rentmebarwaha.property.entity.PropertyStatus;

import java.util.List;
import java.util.UUID;

public interface OwnerPropertyService {

    OwnerPropertySummaryResponse createDraftProperty(UUID ownerId, CreatePropertyRequest request);

    OwnerPropertySummaryResponse updateProperty(UUID ownerId, UUID propertyId, UpdatePropertyRequest request);

    OwnerPropertySummaryResponse publishProperty(UUID ownerId, UUID propertyId);

    PageResponse<OwnerPropertySummaryResponse> getOwnerProperties(UUID ownerId, PropertyStatus status, int page, int size);

    void deleteProperty(UUID ownerId, UUID propertyId);

    List<PropertyImageDto> addImages(UUID ownerId, UUID propertyId, SavePropertyImagesRequest request);

    List<PropertyImageDto> reorderImages(UUID ownerId, UUID propertyId, ReorderImagesRequest request);

    void deleteImage(UUID ownerId, UUID propertyId, UUID imageId);
}

