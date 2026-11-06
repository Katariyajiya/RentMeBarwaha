package com.project.rentmebarwaha.auth.dto;

public record OtpSendResponse(
        String target,
        String message,
        long expiresInSeconds
) {}

