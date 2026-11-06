package com.project.rentmebarwaha.auth;

import com.project.rentmebarwaha.auth.dto.*;
import com.project.rentmebarwaha.auth.entity.OtpToken;
import com.project.rentmebarwaha.auth.entity.RefreshToken;
import com.project.rentmebarwaha.auth.repository.OtpTokenRepository;
import com.project.rentmebarwaha.auth.repository.RefreshTokenRepository;
import com.project.rentmebarwaha.auth.security.JwtProvider;
import com.project.rentmebarwaha.auth.service.AuthServiceImpl;
import com.project.rentmebarwaha.auth.service.GoogleTokenVerifier;
import com.project.rentmebarwaha.auth.service.OtpSender;
import com.project.rentmebarwaha.common.exception.BadRequestException;
import com.project.rentmebarwaha.common.exception.RateLimitException;
import com.project.rentmebarwaha.user.entity.Role;
import com.project.rentmebarwaha.user.entity.User;
import com.project.rentmebarwaha.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpTokenRepository otpTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpSender otpSender;

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .phone("9876543210")
                .role(Role.TENANT)
                .city("Barwaha")
                .active(true)
                .build();
    }

    @Test
    void testSendOtp_Success() {
        when(otpTokenRepository.countByTargetAndCreatedAtAfter(anyString(), any(Instant.class))).thenReturn(1L);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedCode");

        OtpSendResponse response = authService.sendOtp(new OtpSendRequest("9876543210"));

        assertNotNull(response);
        assertEquals("9876543210", response.target());
        verify(otpTokenRepository).save(any(OtpToken.class));
        verify(otpSender).sendOtp(eq("9876543210"), anyString());
    }

    @Test
    void testSendOtp_RateLimitExceeded() {
        when(otpTokenRepository.countByTargetAndCreatedAtAfter(anyString(), any(Instant.class))).thenReturn(3L);

        assertThrows(RateLimitException.class, () -> authService.sendOtp(new OtpSendRequest("9876543210")));
        verify(otpTokenRepository, never()).save(any());
        verify(otpSender, never()).sendOtp(anyString(), anyString());
    }

    @Test
    void testVerifyOtp_Success() {
        OtpToken otpToken = OtpToken.builder()
                .target("9876543210")
                .codeHash("hashedCode")
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .attempts(0)
                .used(false)
                .build();

        when(otpTokenRepository.findFirstByTargetAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(eq("9876543210"), any(Instant.class)))
                .thenReturn(Optional.of(otpToken));
        when(passwordEncoder.matches("123456", "hashedCode")).thenReturn(true);
        when(userRepository.findByPhoneAndDeletedAtIsNull("9876543210")).thenReturn(Optional.of(sampleUser));
        when(jwtProvider.generateAccessToken(any(), any(), any(), any())).thenReturn("mockJwtToken");
        when(jwtProvider.generateRefreshTokenString()).thenReturn("mockRefreshToken");
        when(jwtProvider.getRefreshTokenExpirationMs()).thenReturn(2592000000L);

        LoginResponse response = authService.verifyOtp(new OtpVerifyRequest("9876543210", "123456"));

        assertNotNull(response);
        assertEquals("mockJwtToken", response.accessToken());
        assertEquals("mockRefreshToken", response.refreshToken());
        assertTrue(otpToken.isUsed());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void testVerifyOtp_IncorrectCode() {
        OtpToken otpToken = OtpToken.builder()
                .target("9876543210")
                .codeHash("hashedCode")
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .attempts(1)
                .used(false)
                .build();

        when(otpTokenRepository.findFirstByTargetAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(eq("9876543210"), any(Instant.class)))
                .thenReturn(Optional.of(otpToken));
        when(passwordEncoder.matches("999999", "hashedCode")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.verifyOtp(new OtpVerifyRequest("9876543210", "999999")));
        assertEquals(2, otpToken.getAttempts());
        assertFalse(otpToken.isUsed());
    }

    @Test
    void testVerifyOtp_ExceededAttempts() {
        OtpToken otpToken = OtpToken.builder()
                .target("9876543210")
                .codeHash("hashedCode")
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .attempts(5)
                .used(false)
                .build();

        when(otpTokenRepository.findFirstByTargetAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(eq("9876543210"), any(Instant.class)))
                .thenReturn(Optional.of(otpToken));

        assertThrows(BadRequestException.class, () -> authService.verifyOtp(new OtpVerifyRequest("9876543210", "123456")));
        assertTrue(otpToken.isUsed());
    }

    @Test
    void testGoogleLogin_Success() {
        GoogleTokenVerifier.GoogleUserPayload payload = GoogleTokenVerifier.GoogleUserPayload.builder()
                .googleId("google_12345")
                .email("test@google.com")
                .name("Google User")
                .pictureUrl("https://example.com/avatar.jpg")
                .build();

        when(googleTokenVerifier.verify("valid_google_token")).thenReturn(payload);
        when(userRepository.findByGoogleIdAndDeletedAtIsNull("google_12345")).thenReturn(Optional.empty());
        when(userRepository.findByEmailAndDeletedAtIsNull("test@google.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtProvider.generateAccessToken(any(), any(), any(), any())).thenReturn("mockJwtToken");
        when(jwtProvider.generateRefreshTokenString()).thenReturn("mockRefreshToken");
        when(jwtProvider.getRefreshTokenExpirationMs()).thenReturn(2592000000L);

        LoginResponse response = authService.googleLogin(new GoogleLoginRequest("valid_google_token"));

        assertNotNull(response);
        assertTrue(response.isNewUser());
        assertEquals("mockJwtToken", response.accessToken());
    }
}

