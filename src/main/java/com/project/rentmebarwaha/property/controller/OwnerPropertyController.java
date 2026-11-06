package com.project.rentmebarwaha.property.controller;

import com.project.rentmebarwaha.common.model.ApiResponse;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.common.util.SecurityUtils;
import com.project.rentmebarwaha.property.dto.*;
import com.project.rentmebarwaha.property.entity.PropertyStatus;
import com.project.rentmebarwaha.property.service.OwnerPropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/owner/properties")
@RequiredArgsConstructor
@Tag(name = "Owner Properties", description = "Endpoints for owner listing management, drafts, publishing, and image ordering")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('OWNER')")
public class OwnerPropertyController {

    private final OwnerPropertyService ownerPropertyService;

    @PostMapping
    @Operation(summary = "Create Property Draft", description = "Creates a new property listing in DRAFT status.")
    public ResponseEntity<ApiResponse<OwnerPropertySummaryResponse>> createDraftProperty(
            @Valid @RequestBody CreatePropertyRequest request) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        OwnerPropertySummaryResponse response = ownerPropertyService.createDraftProperty(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Property draft created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Property Listing", description = "Updates draft or active property details.")
    public ResponseEntity<ApiResponse<OwnerPropertySummaryResponse>> updateProperty(
            @PathVariable UUID id,
            @RequestBody UpdatePropertyRequest request) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        OwnerPropertySummaryResponse response = ownerPropertyService.updateProperty(ownerId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Property updated successfully", response));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish Property Listing", description = "Validates completeness (min 3 images, rent, deposit, locality, config); moves to PENDING_REVIEW (or auto-ACTIVE if verified landlord).")
    public ResponseEntity<ApiResponse<OwnerPropertySummaryResponse>> publishProperty(@PathVariable UUID id) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        OwnerPropertySummaryResponse response = ownerPropertyService.publishProperty(ownerId, id);
        return ResponseEntity.ok(ApiResponse.ok("Property published successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get Owner's Properties", description = "Returns properties listed by the authenticated owner, filtered optionally by status.")
    public ResponseEntity<ApiResponse<PageResponse<OwnerPropertySummaryResponse>>> getOwnerProperties(
            @RequestParam(required = false) PropertyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        PageResponse<OwnerPropertySummaryResponse> response = ownerPropertyService.getOwnerProperties(ownerId, status, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Property Listing", description = "Soft-deletes the property listing.")
    public ResponseEntity<ApiResponse<Void>> deleteProperty(@PathVariable UUID id) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        ownerPropertyService.deleteProperty(ownerId, id);
        return ResponseEntity.ok(ApiResponse.ok("Property listing deleted successfully"));
    }

    @PostMapping("/{id}/images")
    @Operation(summary = "Attach Images to Property", description = "Registers uploaded image URLs with the property listing.")
    public ResponseEntity<ApiResponse<List<PropertyImageDto>>> addImages(
            @PathVariable UUID id,
            @Valid @RequestBody SavePropertyImagesRequest request) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        List<PropertyImageDto> response = ownerPropertyService.addImages(ownerId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Images attached successfully", response));
    }

    @PutMapping("/{id}/images/order")
    @Operation(summary = "Reorder Property Images", description = "Reorders images; the first image becomes the listing's cover image.")
    public ResponseEntity<ApiResponse<List<PropertyImageDto>>> reorderImages(
            @PathVariable UUID id,
            @Valid @RequestBody ReorderImagesRequest request) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        List<PropertyImageDto> response = ownerPropertyService.reorderImages(ownerId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Images reordered successfully", response));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @Operation(summary = "Delete Property Image", description = "Deletes a specific property image.")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        ownerPropertyService.deleteImage(ownerId, id, imageId);
        return ResponseEntity.ok(ApiResponse.ok("Image deleted successfully"));
    }
}

