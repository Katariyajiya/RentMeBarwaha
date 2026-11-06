package com.project.rentmebarwaha.user.controller;

import com.project.rentmebarwaha.common.model.ApiResponse;
import com.project.rentmebarwaha.common.util.SecurityUtils;
import com.project.rentmebarwaha.user.dto.RoleSelectRequest;
import com.project.rentmebarwaha.user.dto.UpdateProfileRequest;
import com.project.rentmebarwaha.user.dto.UserProfileResponse;
import com.project.rentmebarwaha.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Endpoints for managing user profile and initial role selection")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @PutMapping("/me/role")
    @Operation(summary = "Select Role (First Login)", description = "Sets initial user role to TENANT or OWNER. Only permitted once upon initial registration.")
    public ResponseEntity<ApiResponse<UserProfileResponse>> selectRole(@Valid @RequestBody RoleSelectRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UserProfileResponse response = userService.setRole(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.ok("Role selected successfully", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get Current User Profile", description = "Returns profile details, verification badges, and role for the authenticated user.")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UserProfileResponse response = userService.getProfile(currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/me")
    @Operation(summary = "Update Current User Profile", description = "Updates editable profile fields (name, phone, avatar URL, city).")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UserProfileResponse response = userService.updateProfile(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", response));
    }
}

