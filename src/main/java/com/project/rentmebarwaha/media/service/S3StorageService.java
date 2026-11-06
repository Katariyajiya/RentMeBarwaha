package com.project.rentmebarwaha.media.service;

import com.project.rentmebarwaha.common.exception.BadRequestException;
import com.project.rentmebarwaha.media.dto.PresignMediaRequest;
import com.project.rentmebarwaha.media.dto.PresignMediaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@ConditionalOnProperty(name = "nestearth.storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    @Value("${nestearth.storage.s3.bucket:nestearth-bucket}")
    private String bucketName;

    @Value("${nestearth.storage.s3.region:ap-south-1}")
    private String region;

    @Override
    public PresignMediaResponse generatePresignedUploadUrl(UUID ownerId, PresignMediaRequest request) {
        if (request.fileSizeBytes() != null && request.fileSizeBytes() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File size exceeds maximum permitted limit of 5 MB");
        }

        String safeFileName = request.fileName().replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectKey = "properties/" + request.propertyId() + "/" + UUID.randomUUID() + "-" + safeFileName;
        String uploadUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, objectKey);
        String publicUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, objectKey);

        return new PresignMediaResponse(
                uploadUrl,
                publicUrl,
                "PUT",
                Map.of("Content-Type", request.contentType()),
                900
        );
    }
}

