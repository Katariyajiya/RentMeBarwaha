package com.project.rentmebarwaha.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpSendRequest(
        @NotBlank(message = "Phone number or email is required")
        String target
) {}

