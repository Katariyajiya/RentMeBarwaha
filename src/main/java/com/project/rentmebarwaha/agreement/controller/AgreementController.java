package com.project.rentmebarwaha.agreement.controller;

import com.project.rentmebarwaha.agreement.dto.AgreementResponse;
import com.project.rentmebarwaha.agreement.dto.CreateAgreementRequest;
import com.project.rentmebarwaha.agreement.entity.AgreementStatus;
import com.project.rentmebarwaha.agreement.service.AgreementService;
import com.project.rentmebarwaha.common.model.ApiResponse;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.common.util.SecurityUtils;
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
@RequestMapping("/api/v1/agreements")
@RequiredArgsConstructor
@Tag(name = "Rental Agreements", description = "Endpoints for drafting, reviewing, and tracking digital zero-brokerage rental agreements")
@SecurityRequirement(name = "bearerAuth")
public class AgreementController {

    private final AgreementService agreementService;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Draft Rental Agreement", description = "Allows property owner to create a formal rental agreement draft for a tenant.")
    public ResponseEntity<ApiResponse<AgreementResponse>> createAgreement(@Valid @RequestBody CreateAgreementRequest request) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        AgreementResponse response = agreementService.createAgreement(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Agreement drafted successfully", response));
    }

    @GetMapping("/mine")
    @Operation(summary = "Get My Agreements", description = "Returns active, drafted, or past rental agreements for the authenticated owner or tenant.")
    public ResponseEntity<ApiResponse<PageResponse<AgreementResponse>>> getMyAgreements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PageResponse<AgreementResponse> response = agreementService.getMyAgreements(userId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Agreement Details", description = "Retrieves full agreement terms, dates, rents, and document URL.")
    public ResponseEntity<ApiResponse<AgreementResponse>> getAgreementById(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AgreementResponse response = agreementService.getAgreementById(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update Agreement Status", description = "Transitions agreement status (e.g. PENDING_SIGNATURE, ACTIVE, TERMINATED).")
    public ResponseEntity<ApiResponse<AgreementResponse>> updateAgreementStatus(
            @PathVariable UUID id,
            @RequestParam AgreementStatus status) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AgreementResponse response = agreementService.updateAgreementStatus(userId, id, status);
        return ResponseEntity.ok(ApiResponse.ok("Agreement status updated successfully", response));
    }
}

