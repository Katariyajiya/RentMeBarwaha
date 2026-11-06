package com.project.rentmebarwaha.visit.controller;

import com.project.rentmebarwaha.common.model.ApiResponse;
import com.project.rentmebarwaha.common.model.PageResponse;
import com.project.rentmebarwaha.common.util.SecurityUtils;
import com.project.rentmebarwaha.visit.dto.ScheduleVisitRequest;
import com.project.rentmebarwaha.visit.dto.UpdateVisitStatusRequest;
import com.project.rentmebarwaha.visit.dto.VisitResponse;
import com.project.rentmebarwaha.visit.service.VisitService;
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
@RequiredArgsConstructor
@Tag(name = "Visits", description = "Endpoints for scheduling, confirming, completing, and tracking physical house visits")
@SecurityRequirement(name = "bearerAuth")
public class VisitController {

    private final VisitService visitService;

    @PostMapping("/api/v1/enquiries/{enquiryId}/visits")
    @Operation(summary = "Schedule Site Visit", description = "Schedules a visit for an existing enquiry. If scheduled by owner, it is CONFIRMED; if by tenant, it is REQUESTED.")
    public ResponseEntity<ApiResponse<VisitResponse>> scheduleVisit(
            @PathVariable UUID enquiryId,
            @Valid @RequestBody ScheduleVisitRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        VisitResponse response = visitService.scheduleVisit(userId, enquiryId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Visit scheduled successfully", response));
    }

    @GetMapping("/api/v1/visits/mine")
    @Operation(summary = "Get Scheduled Visits", description = "Retrieves upcoming and past visits for the authenticated user (tenant or owner).")
    public ResponseEntity<ApiResponse<PageResponse<VisitResponse>>> getMyVisits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PageResponse<VisitResponse> response = visitService.getMyVisits(userId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/v1/visits/{id}")
    @Operation(summary = "Get Visit Details", description = "Retrieves details of a specific visit.")
    public ResponseEntity<ApiResponse<VisitResponse>> getVisitById(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        VisitResponse response = visitService.getVisitById(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/api/v1/visits/{id}/status")
    @Operation(summary = "Update Visit Status", description = "Updates status of a visit (CONFIRMED, COMPLETED, CANCELLED) along with optional notes.")
    public ResponseEntity<ApiResponse<VisitResponse>> updateVisitStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVisitStatusRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        VisitResponse response = visitService.updateVisitStatus(userId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Visit status updated successfully", response));
    }

    @GetMapping("/api/v1/owner/visits/upcoming")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Get Upcoming Visits (Owner)", description = "Retrieves immediate upcoming visits for the owner's dashboard.")
    public ResponseEntity<ApiResponse<List<VisitResponse>>> getUpcomingOwnerVisits(
            @RequestParam(defaultValue = "5") int limit) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        List<VisitResponse> response = visitService.getUpcomingVisitsForOwner(ownerId, limit);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

