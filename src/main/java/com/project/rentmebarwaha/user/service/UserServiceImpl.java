package com.project.rentmebarwaha.user.service;

import com.project.rentmebarwaha.common.exception.BadRequestException;
import com.project.rentmebarwaha.common.exception.ConflictException;
import com.project.rentmebarwaha.common.exception.ResourceNotFoundException;
import com.project.rentmebarwaha.user.dto.RoleSelectRequest;
import com.project.rentmebarwaha.user.dto.UpdateProfileRequest;
import com.project.rentmebarwaha.user.dto.UserProfileResponse;
import com.project.rentmebarwaha.user.entity.Role;
import com.project.rentmebarwaha.user.entity.User;
import com.project.rentmebarwaha.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = getById(userId);
        return UserProfileResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getById(userId);

        if (request.name() != null) {
            user.setName(request.name().trim());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().trim());
        }
        if (request.city() != null) {
            user.setCity(request.city().trim());
        }
        if (request.phone() != null && !request.phone().trim().isEmpty()) {
            String newPhone = request.phone().trim();
            if (!newPhone.equals(user.getPhone())) {
                userRepository.findByPhoneAndDeletedAtIsNull(newPhone).ifPresent(existing -> {
                    if (!existing.getId().equals(userId)) {
                        throw new ConflictException("Phone number " + newPhone + " is already in use by another account");
                    }
                });
                user.setPhone(newPhone);
            }
        }

        User updated = userRepository.save(user);
        log.info("Updated profile for user id: {}", userId);
        return UserProfileResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public UserProfileResponse setRole(UUID userId, RoleSelectRequest request) {
        User user = getById(userId);

        if (user.getRole() != null) {
            throw new BadRequestException("Role has already been selected for this account and cannot be modified via role-select");
        }

        Role selectedRole = request.role();
        if (selectedRole == Role.ADMIN) {
            throw new BadRequestException("ADMIN role cannot be self-selected");
        }

        user.setRole(selectedRole);
        User updated = userRepository.save(user);
        log.info("Assigned role [{}] to user id: {}", selectedRole, userId);
        return UserProfileResponse.fromEntity(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}

