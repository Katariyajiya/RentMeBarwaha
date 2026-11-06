package com.project.rentmebarwaha.property.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SavePropertyImagesRequest(
        @NotEmpty(message = "At least one image URL must be provided")
        List<String> imageUrls
) {}

