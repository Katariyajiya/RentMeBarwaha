package com.project.rentmebarwaha.media.service;

import com.project.rentmebarwaha.media.dto.PresignMediaRequest;
import com.project.rentmebarwaha.media.dto.PresignMediaResponse;

import java.util.UUID;

public interface StorageService {

    PresignMediaResponse generatePresignedUploadUrl(UUID ownerId, PresignMediaRequest request);
}

