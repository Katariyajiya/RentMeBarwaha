package com.project.rentmebarwaha.user;

import com.project.rentmebarwaha.common.exception.BadRequestException;
import com.project.rentmebarwaha.common.exception.ConflictException;
import com.project.rentmebarwaha.common.exception.ResourceNotFoundException;
import com.project.rentmebarwaha.user.dto.RoleSelectRequest;
import com.project.rentmebarwaha.user.dto.UpdateProfileRequest;
import com.project.rentmebarwaha.user.dto.UserProfileResponse;
import com.project.rentmebarwaha.user.entity.Role;
import com.project.rentmebarwaha.user.entity.User;
import com.project.rentmebarwaha.user.repository.UserRepository;
import com.project.rentmebarwaha.user.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(userId)
                .name("Ramesh Kumar")
                .phone("9876543210")
                .email("ramesh@example.com")
                .city("Barwaha")
                .active(true)
                .build();
    }

    @Test
    void testGetProfile_Success() {
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(sampleUser));

        UserProfileResponse response = userService.getProfile(userId);

        assertNotNull(response);
        assertEquals("Ramesh Kumar", response.name());
        assertEquals("Barwaha", response.city());
    }

    @Test
    void testGetProfile_NotFound() {
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getProfile(userId));
    }

    @Test
    void testSelectRole_FirstTime_Success() {
        sampleUser.setRole(null);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileResponse response = userService.setRole(userId, new RoleSelectRequest(Role.TENANT));

        assertNotNull(response);
        assertEquals(Role.TENANT, response.role());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void testSelectRole_AlreadySelected_ThrowsBadRequest() {
        sampleUser.setRole(Role.OWNER);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(sampleUser));

        assertThrows(BadRequestException.class, () -> userService.setRole(userId, new RoleSelectRequest(Role.TENANT)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testSelectRole_AdminRole_ThrowsBadRequest() {
        sampleUser.setRole(null);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(sampleUser));

        assertThrows(BadRequestException.class, () -> userService.setRole(userId, new RoleSelectRequest(Role.ADMIN)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testUpdateProfile_Success() {
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.findByPhoneAndDeletedAtIsNull("9998887776")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("Suresh Sharma", "9998887776", "https://img.com/avatar.png", "Indore");
        UserProfileResponse response = userService.updateProfile(userId, request);

        assertNotNull(response);
        assertEquals("Suresh Sharma", response.name());
        assertEquals("9998887776", response.phone());
        assertEquals("Indore", response.city());
        assertEquals("https://img.com/avatar.png", response.avatarUrl());
    }

    @Test
    void testUpdateProfile_PhoneDuplicate_ThrowsConflict() {
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(sampleUser));
        User otherUser = User.builder().id(UUID.randomUUID()).phone("9998887776").build();
        when(userRepository.findByPhoneAndDeletedAtIsNull("9998887776")).thenReturn(Optional.of(otherUser));

        UpdateProfileRequest request = new UpdateProfileRequest("Suresh Sharma", "9998887776", null, null);
        assertThrows(ConflictException.class, () -> userService.updateProfile(userId, request));
    }
}

