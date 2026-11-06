package com.project.rentmebarwaha.enquiry.controller;

import com.project.rentmebarwaha.common.model.ApiResponse;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.common.util.SecurityUtils;
import com.project.rentmebarwaha.enquiry.dto.EnquiryResponse;
import com.project.rentmebarwaha.enquiry.dto.UpdateEnquiryStatusRequest;
import com.project.rentmebarwaha.enquiry.entity.EnquiryStatus;
import com.project.rentmebarwaha.enquiry.service.EnquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/owner/enquiries")
@RequiredArgsConstructor
@Tag(name = "Enquiries (Owner)", description = "Endpoints for property owners to manage incoming tenant leads and applications")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('OWNER')")
public class OwnerEnquiryController {

    private final EnquiryService enquiryService;

    @GetMapping
    @Operation(summary = "Get Owner's Enquiries", description = "Retrieves enquiries for the owner's properties, optionally filtered by propertyId or status.")
    public ResponseEntity<ApiResponse<PageResponse<EnquiryResponse>>> getOwnerEnquiries(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) EnquiryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        PageResponse<EnquiryResponse> response = enquiryService.getOwnerEnquiries(ownerId, propertyId, status, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Enquiry Lead Details", description = "Retrieves full details of an enquiry for an owner's property.")
    public ResponseEntity<ApiResponse<EnquiryResponse>> getEnquiryById(@PathVariable UUID id) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        EnquiryResponse response = enquiryService.getEnquiryById(ownerId, id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update Enquiry Status", description = "Updates enquiry status (e.g. ACCEPTED, REJECTED, REPLIED, CLOSED). Acceptance reveals contact information.")
    public ResponseEntity<ApiResponse<EnquiryResponse>> updateEnquiryStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEnquiryStatusRequest request) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        EnquiryResponse response = enquiryService.updateEnquiryStatus(ownerId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Enquiry status updated successfully", response));
    }
}

