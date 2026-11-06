package com.project.rentmebarwaha.property.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReorderImagesRequest(
        @NotEmpty(message = "Image IDs in desired order must be provided")
        List<UUID> imageIds
) {}

