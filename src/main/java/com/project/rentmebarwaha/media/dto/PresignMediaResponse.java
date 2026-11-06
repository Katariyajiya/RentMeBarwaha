package com.project.rentmebarwaha.media.dto;

import java.util.Map;

public record PresignMediaResponse(
        String uploadUrl,
        String publicUrl,
        String httpMethod,
        Map<String, String> headers,
        long expiresInSeconds
) {}

