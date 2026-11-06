package com.project.rentmebarwaha.media.controller;

import com.project.rentmebarwaha.common.model.ApiResponse;
import com.project.rentmebarwaha.common.util.SecurityUtils;
import com.project.rentmebarwaha.media.dto.PresignMediaRequest;
import com.project.rentmebarwaha.media.dto.PresignMediaResponse;
import com.project.rentmebarwaha.media.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media Storage", description = "Endpoints for generating presigned image upload URLs (max 5 MB, jpg/png/webp)")
@SecurityRequirement(name = "bearerAuth")
public class MediaController {

    private final StorageService storageService;

    @PostMapping("/presign")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Generate Presigned Media Upload URL", description = "Validates file type (jpg/png/webp) and size (<= 5 MB); returns presigned upload URL and public CDN URL.")
    public ResponseEntity<ApiResponse<PresignMediaResponse>> presignUpload(@Valid @RequestBody PresignMediaRequest request) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        PresignMediaResponse response = storageService.generatePresignedUploadUrl(ownerId, request);
        return ResponseEntity.ok(ApiResponse.ok("Presigned upload URL generated", response));
    }
}

