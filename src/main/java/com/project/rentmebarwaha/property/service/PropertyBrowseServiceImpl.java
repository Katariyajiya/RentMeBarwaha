package com.project.rentmebarwaha.property.service;

import com.project.rentmebarwaha.common.exception.ResourceNotFoundException;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.property.dto.*;
import com.project.rentmebarwaha.property.entity.Property;
import com.project.rentmebarwaha.property.entity.PropertyImage;
import com.project.rentmebarwaha.property.entity.PropertyStatus;
import com.project.rentmebarwaha.property.entity.PropertyView;
import com.project.rentmebarwaha.property.repository.PropertyImageRepository;
import com.project.rentmebarwaha.property.repository.PropertyRepository;
import com.project.rentmebarwaha.property.repository.PropertySpecification;
import com.project.rentmebarwaha.property.repository.PropertyViewRepository;
import com.project.rentmebarwaha.saved.repository.SavedPropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyBrowseServiceImpl implements PropertyBrowseService {

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final PropertyViewRepository propertyViewRepository;
    private final SavedPropertyRepository savedPropertyRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PropertyListItemResponse> searchProperties(PropertyFilterCriteria criteria, UUID currentUserId) {
        int page = criteria != null ? criteria.getPageOrDefault() : 0;
        int size = criteria != null ? criteria.getSizeOrDefault() : 20;

        Sort sort = resolveSort(criteria != null ? criteria.sort() : null);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Property> propertyPage = propertyRepository.findAll(PropertySpecification.build(criteria), pageable);

        // Batch fetch saved IDs for current user to avoid N+1 queries
        Set<UUID> savedPropertyIds = Collections.emptySet();
        if (currentUserId != null && !propertyPage.isEmpty()) {
            List<UUID> pagePropertyIds = propertyPage.getContent().stream().map(Property::getId).toList();
            savedPropertyIds = savedPropertyRepository.findSavedPropertyIdsIn(currentUserId, pagePropertyIds);
        }

        final Set<UUID> finalSavedIds = savedPropertyIds;
        Page<PropertyListItemResponse> itemPage = propertyPage.map(p -> mapToListItem(p, finalSavedIds.contains(p.getId())));

        return PageResponse.of(itemPage);
    }

    @Override
    @Transactional
    public PropertyDetailResponse getPropertyDetail(UUID id, UUID currentUserId, String clientIdentifier) {
        Property property = propertyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property", "id", id));

        // Increment view count with 24-hour deduplication
        recordViewIfEligible(property, currentUserId, clientIdentifier);

        boolean savedByMe = false;
        if (currentUserId != null) {
            savedByMe = savedPropertyRepository.existsByUserIdAndPropertyId(currentUserId, id);
        }

        List<PropertyImage> images = propertyImageRepository.findByPropertyIdOrderBySortOrderAsc(id);
        List<PropertyImageDto> imageDtos = images.stream().map(PropertyImageDto::fromEntity).toList();

        return new PropertyDetailResponse(
                property.getId(),
                property.getTitle(),
                property.getDescription(),
                property.getConfig(),
                property.getLocality(),
                property.getCity(),
                property.getAddress(),
                property.getLatitude(),
                property.getLongitude(),
                property.getAreaSqft(),
                property.getFloor(),
                property.getMonthlyRent(),
                property.getSecurityDeposit(),
                property.getFurnishing(),
                property.getPreferredTenants(),
                property.getAvailableFrom(),
                property.isAvailableImmediately(),
                property.isDirectOwner(),
                property.isFeatured(),
                property.isVerified(),
                property.getStatus(),
                property.getViewCount(),
                property.getPublishedAt(),
                property.getAmenities(),
                property.getTags(),
                imageDtos,
                OwnerCardDto.fromEntity(property.getOwner()),
                savedByMe
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getLocalities(String city) {
        if (city != null && !city.trim().isEmpty()) {
            return propertyRepository.findDistinctLocalitiesByCity(city.trim());
        }
        return propertyRepository.findAllActiveLocalities();
    }

    private void recordViewIfEligible(Property property, UUID currentUserId, String clientIdentifier) {
        String identifier = currentUserId != null ? currentUserId.toString() : (clientIdentifier != null ? clientIdentifier : "anonymous");
        Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);

        boolean alreadyViewed = propertyViewRepository.existsByPropertyIdAndViewerIdentifierAndViewedAtAfter(
                property.getId(), identifier, oneDayAgo
        );

        if (!alreadyViewed) {
            PropertyView view = PropertyView.builder()
                    .property(property)
                    .viewerId(currentUserId)
                    .viewerIdentifier(identifier)
                    .build();
            propertyViewRepository.save(view);

            property.setViewCount(property.getViewCount() + 1);
            propertyRepository.save(property);
        }
    }

    private PropertyListItemResponse mapToListItem(Property property, boolean isSaved) {
        List<PropertyImage> images = property.getImages();
        String coverUrl = null;
        if (images != null && !images.isEmpty()) {
            coverUrl = images.stream()
                    .filter(PropertyImage::isCover)
                    .map(PropertyImage::getUrl)
                    .findFirst()
                    .orElse(images.get(0).getUrl());
        }

        return new PropertyListItemResponse(
                property.getId(),
                property.getTitle(),
                coverUrl,
                property.getMonthlyRent(),
                property.getSecurityDeposit(),
                property.getConfig(),
                property.getFurnishing(),
                property.getAreaSqft(),
                property.getLocality(),
                property.getCity(),
                property.getTags(),
                property.isDirectOwner(),
                property.isVerified(),
                isSaved,
                property.getViewCount(),
                property.isAvailableImmediately(),
                property.getAvailableFrom()
        );
    }

    private Sort resolveSort(String sortParam) {
        if ("rentAsc".equalsIgnoreCase(sortParam)) {
            return Sort.by(Sort.Direction.ASC, "monthlyRent");
        } else if ("rentDesc".equalsIgnoreCase(sortParam)) {
            return Sort.by(Sort.Direction.DESC, "monthlyRent");
        } else {
            // Default newest
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
    }
}

