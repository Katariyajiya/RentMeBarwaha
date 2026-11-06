package com.project.rentmebarwaha.user.service;

import com.project.rentmebarwaha.user.dto.RoleSelectRequest;
import com.project.rentmebarwaha.user.dto.UpdateProfileRequest;
import com.project.rentmebarwaha.user.dto.UserProfileResponse;
import com.project.rentmebarwaha.user.entity.User;

import java.util.UUID;

public interface UserService {

    UserProfileResponse getProfile(UUID userId);

    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);

    UserProfileResponse setRole(UUID userId, RoleSelectRequest request);

    User getById(UUID userId);
}

