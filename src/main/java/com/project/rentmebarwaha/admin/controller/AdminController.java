package com.project.rentmebarwaha.admin.controller;

import com.project.rentmebarwaha.admin.dto.AdminPropertyReviewRequest;
import com.project.rentmebarwaha.admin.service.AdminService;
import com.project.rentmebarwaha.common.model.ApiResponse;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.property.dto.OwnerPropertySummaryResponse;
import com.project.rentmebarwaha.user.dto.UserProfileResponse;
import com.project.rentmebarwaha.user.entity.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Operations", description = "Endpoints for platform administrators to review listings, verify landlord badges, and audit accounts")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/properties/pending")
    @Operation(summary = "Get Pending Listings", description = "Retrieves listings waiting in PENDING_REVIEW status for moderation.")
    public ResponseEntity<ApiResponse<PageResponse<OwnerPropertySummaryResponse>>> getPendingProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<OwnerPropertySummaryResponse> response = adminService.getPendingProperties(page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/properties/{id}/approve")
    @Operation(summary = "Approve Listing", description = "Approves a pending listing, moving its status to ACTIVE and notifying the owner.")
    public ResponseEntity<ApiResponse<OwnerPropertySummaryResponse>> approveProperty(@PathVariable UUID id) {
        OwnerPropertySummaryResponse response = adminService.approveProperty(id);
        return ResponseEntity.ok(ApiResponse.ok("Property approved and published to active listings", response));
    }

    @PatchMapping("/properties/{id}/reject")
    @Operation(summary = "Reject Listing", description = "Rejects a pending listing, returning it to DRAFT with feedback for the owner.")
    public ResponseEntity<ApiResponse<OwnerPropertySummaryResponse>> rejectProperty(
            @PathVariable UUID id,
            @RequestBody(required = false) AdminPropertyReviewRequest request) {
        OwnerPropertySummaryResponse response = adminService.rejectProperty(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Property rejected with feedback", response));
    }

    @PatchMapping("/users/{id}/verify-owner")
    @Operation(summary = "Set Owner Verification Badge", description = "Grants or revokes the verified landlord badge. Verified owners auto-publish listings without review.")
    public ResponseEntity<ApiResponse<UserProfileResponse>> verifyOwner(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean verified) {
        UserProfileResponse response = adminService.verifyOwner(id, verified);
        return ResponseEntity.ok(ApiResponse.ok("Owner verification badge updated", response));
    }

    @PatchMapping("/users/{id}/verify-kyc")
    @Operation(summary = "Set KYC Verification Badge", description = "Approves or rejects user identity (KYC) verification.")
    public ResponseEntity<ApiResponse<UserProfileResponse>> verifyKyc(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "true") boolean verified) {
        UserProfileResponse response = adminService.verifyKyc(id, verified);
        return ResponseEntity.ok(ApiResponse.ok("User KYC verification updated", response));
    }

    @GetMapping("/users")
    @Operation(summary = "List Users", description = "Returns registered users, optionally filtered by role.")
    public ResponseEntity<ApiResponse<PageResponse<UserProfileResponse>>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<UserProfileResponse> response = adminService.getUsers(role, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

