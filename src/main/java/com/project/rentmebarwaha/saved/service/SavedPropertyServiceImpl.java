package com.project.rentmebarwaha.saved.service;

import com.project.rentmebarwaha.common.exception.ResourceNotFoundException;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.property.dto.PropertyListItemResponse;
import com.project.rentmebarwaha.property.entity.Property;
import com.project.rentmebarwaha.property.entity.PropertyImage;
import com.project.rentmebarwaha.property.entity.PropertyStatus;
import com.project.rentmebarwaha.property.repository.PropertyRepository;
import com.project.rentmebarwaha.saved.entity.SavedProperty;
import com.project.rentmebarwaha.saved.repository.SavedPropertyRepository;
import com.project.rentmebarwaha.user.entity.User;
import com.project.rentmebarwaha.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SavedPropertyServiceImpl implements SavedPropertyService {

    private final SavedPropertyRepository savedPropertyRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void saveProperty(UUID userId, UUID propertyId) {
        if (savedPropertyRepository.existsByUserIdAndPropertyId(userId, propertyId)) {
            return; // Idempotent
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Property property = propertyRepository.findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", "id", propertyId));

        SavedProperty saved = SavedProperty.builder()
                .user(user)
                .property(property)
                .build();

        savedPropertyRepository.save(saved);
        log.info("User {} saved property {}", userId, propertyId);
    }

    @Override
    @Transactional
    public void unsaveProperty(UUID userId, UUID propertyId) {
        savedPropertyRepository.deleteByUserIdAndPropertyId(userId, propertyId);
        log.info("User {} unsaved property {}", userId, propertyId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PropertyListItemResponse> getSavedProperties(UUID userId, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        Page<SavedProperty> savedPage = savedPropertyRepository.findByUserIdAndPropertyStatus(userId, PropertyStatus.ACTIVE, pageable);

        Page<PropertyListItemResponse> itemPage = savedPage.map(s -> {
            Property p = s.getProperty();
            List<PropertyImage> images = p.getImages();
            String coverUrl = null;
            if (images != null && !images.isEmpty()) {
                coverUrl = images.stream().filter(PropertyImage::isCover).map(PropertyImage::getUrl).findFirst()
                        .orElse(images.get(0).getUrl());
            }

            return new PropertyListItemResponse(
                    p.getId(),
                    p.getTitle(),
                    coverUrl,
                    p.getMonthlyRent(),
                    p.getSecurityDeposit(),
                    p.getConfig(),
                    p.getFurnishing(),
                    p.getAreaSqft(),
                    p.getLocality(),
                    p.getCity(),
                    p.getTags(),
                    p.isDirectOwner(),
                    p.isVerified(),
                    true,
                    p.getViewCount(),
                    p.isAvailableImmediately(),
                    p.getAvailableFrom()
            );
        });

        return PageResponse.of(itemPage);
    }
}

