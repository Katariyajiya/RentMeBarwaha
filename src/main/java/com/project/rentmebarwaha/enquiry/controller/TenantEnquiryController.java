package com.project.rentmebarwaha.enquiry.controller;

import com.project.rentmebarwaha.common.model.ApiResponse;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.common.util.SecurityUtils;
import com.project.rentmebarwaha.enquiry.dto.CreateEnquiryRequest;
import com.project.rentmebarwaha.enquiry.dto.EnquiryResponse;
import com.project.rentmebarwaha.enquiry.service.EnquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Enquiries (Tenant)", description = "Endpoints for home seekers to enquire about properties and track their applications")
@SecurityRequirement(name = "bearerAuth")
public class TenantEnquiryController {

    private final EnquiryService enquiryService;

    @PostMapping("/api/v1/properties/{id}/enquiries")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    @Operation(summary = "Submit Enquiry", description = "Submits a rental enquiry for an active property listing. Rate limited to 1 submission per property per 24 hours.")
    public ResponseEntity<ApiResponse<EnquiryResponse>> createEnquiry(
            @PathVariable UUID id,
            @Valid @RequestBody CreateEnquiryRequest request) {
        UUID tenantId = SecurityUtils.getCurrentUserId();
        EnquiryResponse response = enquiryService.createEnquiry(tenantId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Enquiry submitted successfully", response));
    }

    @GetMapping("/api/v1/enquiries/mine")
    @PreAuthorize("hasAnyRole('TENANT', 'ADMIN')")
    @Operation(summary = "Get Tenant's Enquiries", description = "Retrieves all enquiries submitted by the authenticated tenant. Owner contact number is revealed only if enquiry is ACCEPTED.")
    public ResponseEntity<ApiResponse<PageResponse<EnquiryResponse>>> getTenantEnquiries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = SecurityUtils.getCurrentUserId();
        PageResponse<EnquiryResponse> response = enquiryService.getTenantEnquiries(tenantId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/v1/enquiries/{id}")
    @Operation(summary = "Get Enquiry Details", description = "Retrieves details of a specific enquiry if the user is the tenant or the owner.")
    public ResponseEntity<ApiResponse<EnquiryResponse>> getEnquiryById(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        EnquiryResponse response = enquiryService.getEnquiryById(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

