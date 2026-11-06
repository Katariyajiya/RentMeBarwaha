package com.project.rentmebarwaha.property.controller;

import com.project.rentmebarwaha.common.model.ApiResponse;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.common.util.SecurityUtils;
import com.project.rentmebarwaha.property.dto.PropertyDetailResponse;
import com.project.rentmebarwaha.property.dto.PropertyFilterCriteria;
import com.project.rentmebarwaha.property.dto.PropertyListItemResponse;
import com.project.rentmebarwaha.property.entity.Furnishing;
import com.project.rentmebarwaha.property.entity.PreferredTenants;
import com.project.rentmebarwaha.property.entity.PropertyConfig;
import com.project.rentmebarwaha.property.service.PropertyBrowseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
@Tag(name = "Property Browsing (Public)", description = "Endpoints for searching and exploring active rental properties without brokerage")
public class PropertyBrowseController {

    private final PropertyBrowseService propertyBrowseService;

    @GetMapping
    @Operation(summary = "Search Active Properties", description = "Filters ACTIVE properties by search query (q), city, locality, rent range, BHK configuration, furnishing, preferred tenants, and sorting.")
    public ResponseEntity<ApiResponse<PageResponse<PropertyListItemResponse>>> searchProperties(
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "Barwaha") String city,
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) BigDecimal minRent,
            @RequestParam(required = false) BigDecimal maxRent,
            @RequestParam(required = false) List<PropertyConfig> config,
            @RequestParam(required = false) List<Furnishing> furnishing,
            @RequestParam(required = false) PreferredTenants preferredTenants,
            @RequestParam(required = false) Boolean availableImmediately,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID currentUserId = SecurityUtils.getOptionalCurrentUserId().orElse(null);

        PropertyFilterCriteria criteria = new PropertyFilterCriteria(
                q, city, locality, minRent, maxRent, config, furnishing, preferredTenants, availableImmediately, sort, page, size
        );

        PageResponse<PropertyListItemResponse> response = propertyBrowseService.searchProperties(criteria, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Property Details", description = "Retrieves complete property details, amenities, tags, images, and owner card (owner phone hidden for privacy). Increments view count (deduped per day).")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> getPropertyDetail(
            @PathVariable UUID id,
            HttpServletRequest request) {
        UUID currentUserId = SecurityUtils.getOptionalCurrentUserId().orElse(null);
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.trim().isEmpty()) {
            clientIp = request.getRemoteAddr();
        }

        PropertyDetailResponse response = propertyBrowseService.getPropertyDetail(id, currentUserId, clientIp);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

