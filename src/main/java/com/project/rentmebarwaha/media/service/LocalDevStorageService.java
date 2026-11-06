package com.project.rentmebarwaha.media.service;

import com.project.rentmebarwaha.common.exception.BadRequestException;
import com.project.rentmebarwaha.media.dto.PresignMediaRequest;
import com.project.rentmebarwaha.media.dto.PresignMediaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@ConditionalOnProperty(name = "nestearth.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalDevStorageService implements StorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    @Override
    public PresignMediaResponse generatePresignedUploadUrl(UUID ownerId, PresignMediaRequest request) {
        if (request.fileSizeBytes() != null && request.fileSizeBytes() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File size exceeds maximum permitted limit of 5 MB (5,242,880 bytes)");
        }

        String safeFileName = request.fileName().replaceAll("[^a-zA-Z0-9._-]", "_");
        String uniqueKey = "properties/" + request.propertyId() + "/" + UUID.randomUUID() + "-" + safeFileName;
        String uploadUrl = "https://mock-s3.nestearth.in/" + uniqueKey + "?uploadSignature=" + UUID.randomUUID();
        String publicUrl = "https://cdn.nestearth.in/" + uniqueKey;

        log.info("Generated local mock presigned upload URL for property {}: {}", request.propertyId(), uploadUrl);

        return new PresignMediaResponse(
                uploadUrl,
                publicUrl,
                "PUT",
                Map.of("Content-Type", request.contentType()),
                900 // 15 min
        );
    }
}

