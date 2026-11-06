package com.project.rentmebarwaha.saved.controller;

import com.project.rentmebarwaha.common.model.ApiResponse;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.common.util.SecurityUtils;
import com.project.rentmebarwaha.property.dto.PropertyListItemResponse;
import com.project.rentmebarwaha.saved.service.SavedPropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Saved Properties", description = "Endpoints for bookmarking and retrieving saved properties for home seekers")
@SecurityRequirement(name = "bearerAuth")
public class SavedPropertyController {

    private final SavedPropertyService savedPropertyService;

    @PostMapping("/api/v1/properties/{id}/save")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    @Operation(summary = "Save Property", description = "Bookmarks a property for the authenticated tenant.")
    public ResponseEntity<ApiResponse<Void>> saveProperty(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        savedPropertyService.saveProperty(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Property saved successfully"));
    }

    @DeleteMapping("/api/v1/properties/{id}/save")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    @Operation(summary = "Remove Saved Property", description = "Removes a property from saved bookmarks.")
    public ResponseEntity<ApiResponse<Void>> unsaveProperty(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        savedPropertyService.unsaveProperty(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Property removed from saved listings"));
    }

    @GetMapping("/api/v1/saved")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    @Operation(summary = "Get Saved Properties", description = "Returns active properties saved by the authenticated tenant.")
    public ResponseEntity<ApiResponse<PageResponse<PropertyListItemResponse>>> getSavedProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PageResponse<PropertyListItemResponse> response = savedPropertyService.getSavedProperties(userId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

