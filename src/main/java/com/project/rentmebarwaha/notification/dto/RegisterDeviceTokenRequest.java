package com.project.rentmebarwaha.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterDeviceTokenRequest(
        @NotBlank(message = "FCM token is required")
        String fcmToken,

        String deviceType
) {}

