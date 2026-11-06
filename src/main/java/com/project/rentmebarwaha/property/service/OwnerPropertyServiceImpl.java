package com.project.rentmebarwaha.property.service;

import com.project.rentmebarwaha.common.exception.BadRequestException;
import com.project.rentmebarwaha.common.exception.ForbiddenException;
import com.project.rentmebarwaha.common.exception.ResourceNotFoundException;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.enquiry.repository.EnquiryRepository;
import com.project.rentmebarwaha.property.dto.*;
import com.project.rentmebarwaha.property.entity.Property;
import com.project.rentmebarwaha.property.entity.PropertyImage;
import com.project.rentmebarwaha.property.entity.PropertyStatus;
import com.project.rentmebarwaha.property.repository.PropertyImageRepository;
import com.project.rentmebarwaha.property.repository.PropertyRepository;
import com.project.rentmebarwaha.user.entity.User;
import com.project.rentmebarwaha.user.repository.UserRepository;
import com.project.rentmebarwaha.visit.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerPropertyServiceImpl implements OwnerPropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final UserRepository userRepository;
    private final EnquiryRepository enquiryRepository;
    private final VisitRepository visitRepository;

    @Override
    @Transactional
    public OwnerPropertySummaryResponse createDraftProperty(UUID ownerId, CreatePropertyRequest request) {
        User owner = userRepository.findByIdAndDeletedAtIsNull(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));

        Property property = Property.builder()
                .owner(owner)
                .title(request.title().trim())
                .description(request.description())
                .config(request.config())
                .locality(request.locality().trim())
                .city(request.city() != null ? request.city().trim() : "Barwaha")
                .address(request.address().trim())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .areaSqft(request.areaSqft())
                .floor(request.floor() != null ? request.floor() : 0)
                .monthlyRent(request.monthlyRent())
                .securityDeposit(request.securityDeposit() != null ? request.securityDeposit() : BigDecimal.ZERO)
                .furnishing(request.furnishing())
                .preferredTenants(request.preferredTenants() != null ? request.preferredTenants() : com.project.rentmebarwaha.property.entity.PreferredTenants.ANY)
                .availableFrom(request.availableFrom())
                .availableImmediately(Boolean.TRUE.equals(request.availableImmediately()))
                .directOwner(request.directOwner() == null || request.directOwner())
                .status(PropertyStatus.DRAFT)
                .amenities(request.amenities() != null ? new HashSet<>(request.amenities()) : new HashSet<>())
                .tags(request.tags() != null ? new HashSet<>(request.tags()) : new HashSet<>())
                .build();

        Property saved = propertyRepository.save(property);
        log.info("Created draft property id: {} by owner: {}", saved.getId(), ownerId);
        return mapToOwnerSummary(saved);
    }

    @Override
    @Transactional
    public OwnerPropertySummaryResponse updateProperty(UUID ownerId, UUID propertyId, UpdatePropertyRequest request) {
        Property property = getPropertyAndVerifyOwnership(propertyId, ownerId);

        if (request.title() != null) property.setTitle(request.title().trim());
        if (request.description() != null) property.setDescription(request.description());
        if (request.config() != null) property.setConfig(request.config());
        if (request.locality() != null) property.setLocality(request.locality().trim());
        if (request.city() != null) property.setCity(request.city().trim());
        if (request.address() != null) property.setAddress(request.address().trim());
        if (request.latitude() != null) property.setLatitude(request.latitude());
        if (request.longitude() != null) property.setLongitude(request.longitude());
        if (request.areaSqft() != null) property.setAreaSqft(request.areaSqft());
        if (request.floor() != null) property.setFloor(request.floor());
        if (request.monthlyRent() != null) property.setMonthlyRent(request.monthlyRent());
        if (request.securityDeposit() != null) property.setSecurityDeposit(request.securityDeposit());
        if (request.furnishing() != null) property.setFurnishing(request.furnishing());
        if (request.preferredTenants() != null) property.setPreferredTenants(request.preferredTenants());
        if (request.availableFrom() != null) property.setAvailableFrom(request.availableFrom());
        if (request.availableImmediately() != null) property.setAvailableImmediately(request.availableImmediately());
        if (request.directOwner() != null) property.setDirectOwner(request.directOwner());
        if (request.amenities() != null) property.setAmenities(new HashSet<>(request.amenities()));
        if (request.tags() != null) property.setTags(new HashSet<>(request.tags()));

        Property updated = propertyRepository.save(property);
        log.info("Updated property id: {} by owner: {}", propertyId, ownerId);
        return mapToOwnerSummary(updated);
    }

    @Override
    @Transactional
    public OwnerPropertySummaryResponse publishProperty(UUID ownerId, UUID propertyId) {
        Property property = getPropertyAndVerifyOwnership(propertyId, ownerId);

        // Validation Rule 1: Minimum 3 images required
        long imageCount = propertyImageRepository.countByPropertyId(propertyId);
        if (imageCount < 3) {
            throw new BadRequestException("A property cannot be published with fewer than 3 images. Currently attached: " + imageCount);
        }

        // Validation Rule 2: Rent and Deposit
        if (property.getMonthlyRent() == null || property.getMonthlyRent().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Monthly rent must be greater than zero before publishing");
        }
        if (property.getSecurityDeposit() == null || property.getSecurityDeposit().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Security deposit must be zero or positive before publishing");
        }

        // Validation Rule 3: Locality, config, furnishing
        if (property.getLocality() == null || property.getLocality().trim().isEmpty()) {
            throw new BadRequestException("Locality is required before publishing");
        }
        if (property.getConfig() == null) {
            throw new BadRequestException("BHK configuration is required before publishing");
        }
        if (property.getFurnishing() == null) {
            throw new BadRequestException("Furnishing status is required before publishing");
        }

        // Fast-track activation if owner is verified
        User owner = property.getOwner();
        if (owner.isOwnerVerified()) {
            property.setStatus(PropertyStatus.ACTIVE);
            property.setVerified(true);
            log.info("Owner {} is verified. Property {} auto-promoted to ACTIVE.", ownerId, propertyId);
        } else {
            property.setStatus(PropertyStatus.PENDING_REVIEW);
            log.info("Owner {} is not verified. Property {} moved to PENDING_REVIEW.", ownerId, propertyId);
        }

        property.setPublishedAt(Instant.now());
        Property published = propertyRepository.save(property);
        return mapToOwnerSummary(published);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OwnerPropertySummaryResponse> getOwnerProperties(UUID ownerId, PropertyStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Property> propertyPage = (status != null)
                ? propertyRepository.findByOwnerIdAndStatusAndDeletedAtIsNull(ownerId, status, pageable)
                : propertyRepository.findByOwnerIdAndDeletedAtIsNull(ownerId, pageable);

        return PageResponse.of(propertyPage.map(this::mapToOwnerSummary));
    }

    @Override
    @Transactional
    public void deleteProperty(UUID ownerId, UUID propertyId) {
        Property property = getPropertyAndVerifyOwnership(propertyId, ownerId);
        property.softDelete();
        property.setStatus(PropertyStatus.ARCHIVED);
        propertyRepository.save(property);
        log.info("Soft deleted property id: {} by owner: {}", propertyId, ownerId);
    }

    @Override
    @Transactional
    public List<PropertyImageDto> addImages(UUID ownerId, UUID propertyId, SavePropertyImagesRequest request) {
        Property property = getPropertyAndVerifyOwnership(propertyId, ownerId);

        List<PropertyImage> existingImages = propertyImageRepository.findByPropertyIdOrderBySortOrderAsc(propertyId);
        int startingOrder = existingImages.size();
        boolean hasCover = existingImages.stream().anyMatch(PropertyImage::isCover);

        List<PropertyImage> newImages = new ArrayList<>();
        for (int i = 0; i < request.imageUrls().size(); i++) {
            String url = request.imageUrls().get(i).trim();
            boolean isCover = (!hasCover && i == 0);
            PropertyImage img = PropertyImage.builder()
                    .property(property)
                    .url(url)
                    .sortOrder(startingOrder + i)
                    .isCover(isCover)
                    .build();
            newImages.add(img);
        }

        propertyImageRepository.saveAll(newImages);
        return propertyImageRepository.findByPropertyIdOrderBySortOrderAsc(propertyId)
                .stream().map(PropertyImageDto::fromEntity).toList();
    }

    @Override
    @Transactional
    public List<PropertyImageDto> reorderImages(UUID ownerId, UUID propertyId, ReorderImagesRequest request) {
        getPropertyAndVerifyOwnership(propertyId, ownerId);

        List<PropertyImage> existingImages = propertyImageRepository.findByPropertyIdOrderBySortOrderAsc(propertyId);
        Map<UUID, PropertyImage> imageMap = new HashMap<>();
        for (PropertyImage img : existingImages) {
            imageMap.put(img.getId(), img);
        }

        List<UUID> orderList = request.imageIds();
        for (int i = 0; i < orderList.size(); i++) {
            UUID imgId = orderList.get(i);
            PropertyImage img = imageMap.get(imgId);
            if (img != null) {
                img.setSortOrder(i);
                img.setCover(i == 0); // First item is always cover
            }
        }

        propertyImageRepository.saveAll(existingImages);
        return propertyImageRepository.findByPropertyIdOrderBySortOrderAsc(propertyId)
                .stream().map(PropertyImageDto::fromEntity).toList();
    }

    @Override
    @Transactional
    public void deleteImage(UUID ownerId, UUID propertyId, UUID imageId) {
        getPropertyAndVerifyOwnership(propertyId, ownerId);
        PropertyImage image = propertyImageRepository.findByIdAndPropertyId(imageId, propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("PropertyImage", "id", imageId));

        boolean wasCover = image.isCover();
        propertyImageRepository.delete(image);

        // If cover was deleted, reassign cover to first remaining image
        if (wasCover) {
            List<PropertyImage> remaining = propertyImageRepository.findByPropertyIdOrderBySortOrderAsc(propertyId);
            if (!remaining.isEmpty()) {
                PropertyImage newCover = remaining.get(0);
                newCover.setCover(true);
                propertyImageRepository.save(newCover);
            }
        }
    }

    private Property getPropertyAndVerifyOwnership(UUID propertyId, UUID ownerId) {
        Property property = propertyRepository.findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", "id", propertyId));

        if (!property.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not have permission to modify this property listing");
        }
        return property;
    }

    private OwnerPropertySummaryResponse mapToOwnerSummary(Property p) {
        List<PropertyImage> images = propertyImageRepository.findByPropertyIdOrderBySortOrderAsc(p.getId());
        String coverUrl = images.stream().filter(PropertyImage::isCover).map(PropertyImage::getUrl).findFirst()
                .orElse(!images.isEmpty() ? images.get(0).getUrl() : null);

        List<PropertyImageDto> imageDtos = images.stream().map(PropertyImageDto::fromEntity).toList();

        long visitCount = p.getId() != null ? visitRepository.countByPropertyId(p.getId()) : 0;
        long enquiryCount = p.getId() != null ? enquiryRepository.countByPropertyIdAndDeletedAtIsNull(p.getId()) : 0;

        return new OwnerPropertySummaryResponse(
                p.getId(),
                p.getTitle(),
                p.getConfig(),
                p.getLocality(),
                p.getCity(),
                p.getAddress(),
                p.getMonthlyRent(),
                p.getSecurityDeposit(),
                p.getFurnishing(),
                p.getStatus(),
                p.isVerified(),
                p.getViewCount(),
                visitCount,
                enquiryCount,
                coverUrl,
                imageDtos,
                p.getAmenities(),
                p.getTags(),
                p.getPublishedAt(),
                p.getCreatedAt()
        );
    }
}

