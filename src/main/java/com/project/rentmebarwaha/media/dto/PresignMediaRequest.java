package com.project.rentmebarwaha.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record PresignMediaRequest(
        @NotNull(message = "Property ID is required")
        UUID propertyId,

        @NotBlank(message = "File name is required")
        String fileName,

        @NotBlank(message = "Content type is required")
        @Pattern(regexp = "^(image/jpeg|image/png|image/webp)$", message = "Only image/jpeg, image/png, and image/webp are supported")
        String contentType,

        Long fileSizeBytes
) {}

