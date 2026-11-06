package com.project.rentmebarwaha.rental.controller;

import com.project.rentmebarwaha.common.model.ApiResponse;
import com.project.rentmebarwaha.common.util.SecurityUtils;
import com.project.rentmebarwaha.rental.dto.MarkRentedRequest;
import com.project.rentmebarwaha.rental.dto.RentalRecordResponse;
import com.project.rentmebarwaha.rental.service.RentalService;
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
@RequestMapping("/api/v1/owner/properties/{id}")
@RequiredArgsConstructor
@Tag(name = "Property Rental & Leases", description = "Endpoints for owners to mark listings as rented and access lease closing records")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('OWNER')")
public class OwnerRentalController {

    private final RentalService rentalService;

    @PostMapping("/rented")
    @Operation(summary = "Mark Property as Rented", description = "Marks a property listing as RENTED with zero brokerage record keeping, and optionally auto-archives open leads.")
    public ResponseEntity<ApiResponse<RentalRecordResponse>> markPropertyAsRented(
            @PathVariable UUID id,
            @Valid @RequestBody MarkRentedRequest request) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        RentalRecordResponse response = rentalService.markPropertyAsRented(ownerId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Property marked as rented successfully", response));
    }

    @GetMapping("/rental-record")
    @Operation(summary = "Get Property Rental Record", description = "Retrieves the rental closing record for a rented property.")
    public ResponseEntity<ApiResponse<RentalRecordResponse>> getRentalRecord(@PathVariable UUID id) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        RentalRecordResponse response = rentalService.getRentalRecordByProperty(ownerId, id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

