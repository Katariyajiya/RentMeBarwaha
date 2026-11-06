package com.project.rentmebarwaha.auth.controller;

import com.project.rentmebarwaha.auth.dto.*;
import com.project.rentmebarwaha.auth.service.AuthService;
import com.project.rentmebarwaha.common.model.ApiResponse;
import com.project.rentmebarwaha.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for OTP login, Google OAuth, token refresh and logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/otp/send")
    @Operation(summary = "Send OTP", description = "Generates and sends a 6-digit OTP code to the given phone or email (rate limited to 3 per 10m).")
    public ResponseEntity<ApiResponse<OtpSendResponse>> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        OtpSendResponse response = authService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("OTP dispatched", response));
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify OTP", description = "Verifies 6-digit OTP code and returns access token (15m), refresh token (30d), and user role info.")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        LoginResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("Authentication successful", response));
    }

    @PostMapping("/google")
    @Operation(summary = "Google OAuth Login", description = "Authenticates or registers a user via Google ID Token.")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        LoginResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(ApiResponse.ok("Google authentication successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT Access Token", description = "Rotates refresh token and issues a fresh 15-minute access token.")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes user refresh token.", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) LogoutRequest request) {
        UUID userId = SecurityUtils.getOptionalCurrentUserId().orElse(null);
        authService.logout(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }
}

