package com.project.rentmebarwaha.auth.service;

import com.project.rentmebarwaha.auth.dto.*;
import com.project.rentmebarwaha.auth.entity.OtpToken;
import com.project.rentmebarwaha.auth.entity.RefreshToken;
import com.project.rentmebarwaha.auth.repository.OtpTokenRepository;
import com.project.rentmebarwaha.auth.repository.RefreshTokenRepository;
import com.project.rentmebarwaha.auth.security.JwtProvider;
import com.project.rentmebarwaha.common.exception.BadRequestException;
import com.project.rentmebarwaha.common.exception.RateLimitException;
import com.project.rentmebarwaha.common.exception.UnauthorizedException;
import com.project.rentmebarwaha.user.dto.UserSummaryDto;
import com.project.rentmebarwaha.user.entity.User;
import com.project.rentmebarwaha.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final OtpSender otpSender;
    private final GoogleTokenVerifier googleTokenVerifier;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${nestearth.otp.fixed-dev-code:}")
    private String fixedDevCode;

    @Override
    @Transactional
    public OtpSendResponse sendOtp(OtpSendRequest request) {
        String target = normalizeTarget(request.target());

        // 1. Rate Limiting: max 3 sends per 10 minutes
        Instant tenMinutesAgo = Instant.now().minus(10, ChronoUnit.MINUTES);
        long recentSends = otpTokenRepository.countByTargetAndCreatedAtAfter(target, tenMinutesAgo);
        if (recentSends >= 3) {
            throw new RateLimitException("Too many OTP requests. Maximum 3 requests allowed per 10 minutes.");
        }

        // 2. Generate 6-digit code
        String code;
        if (fixedDevCode != null && !fixedDevCode.trim().isEmpty() && "123456".equals(fixedDevCode)) {
            code = fixedDevCode;
        } else {
            code = String.format("%06d", secureRandom.nextInt(1_000_000));
        }

        // 3. Hash code and save token with 5-minute expiry
        String codeHash = passwordEncoder.encode(code);
        Instant expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);

        OtpToken otpToken = OtpToken.builder()
                .target(target)
                .codeHash(codeHash)
                .expiresAt(expiresAt)
                .attempts(0)
                .used(false)
                .build();

        otpTokenRepository.save(otpToken);

        // 4. Dispatch via provider interface
        otpSender.sendOtp(target, code);

        return new OtpSendResponse(target, "OTP sent successfully. Valid for 5 minutes.", 300);
    }

    @Override
    @Transactional
    public LoginResponse verifyOtp(OtpVerifyRequest request) {
        String target = normalizeTarget(request.target());
        String code = request.code().trim();

        // Find active valid OTP token
        OtpToken otpToken = otpTokenRepository
                .findFirstByTargetAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(target, Instant.now())
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP. Please request a new OTP."));

        // Check attempts limit (max 5)
        otpToken.setAttempts(otpToken.getAttempts() + 1);
        if (otpToken.getAttempts() > 5) {
            otpToken.setUsed(true);
            otpTokenRepository.save(otpToken);
            throw new BadRequestException("Maximum OTP verification attempts exceeded. Please request a new OTP.");
        }

        // Verify hash
        boolean matches = passwordEncoder.matches(code, otpToken.getCodeHash()) || (fixedDevCode != null && fixedDevCode.equals(code));
        if (!matches) {
            otpTokenRepository.save(otpToken);
            throw new BadRequestException("Incorrect OTP code. Attempts remaining: " + (5 - otpToken.getAttempts()));
        }

        // Mark OTP as used
        otpToken.setUsed(true);
        otpTokenRepository.save(otpToken);

        // Find or create user
        boolean isPhone = isPhoneNumber(target);
        Optional<User> existingUserOpt = isPhone
                ? userRepository.findByPhoneAndDeletedAtIsNull(target)
                : userRepository.findByEmailAndDeletedAtIsNull(target);

        boolean isNewUser = existingUserOpt.isEmpty() || existingUserOpt.get().getRole() == null;
        User user = existingUserOpt.orElseGet(() -> {
            User newUser = User.builder()
                    .phone(isPhone ? target : null)
                    .email(!isPhone ? target : null)
                    .city("Barwaha")
                    .active(true)
                    .build();
            return userRepository.save(newUser);
        });

        return createLoginResponse(user, isNewUser);
    }

    @Override
    @Transactional
    public LoginResponse googleLogin(GoogleLoginRequest request) {
        GoogleTokenVerifier.GoogleUserPayload payload = googleTokenVerifier.verify(request.idToken());

        Optional<User> userOpt = userRepository.findByGoogleIdAndDeletedAtIsNull(payload.getGoogleId());
        if (userOpt.isEmpty() && payload.getEmail() != null) {
            userOpt = userRepository.findByEmailAndDeletedAtIsNull(payload.getEmail());
        }

        boolean isNewUser = userOpt.isEmpty() || userOpt.get().getRole() == null;
        User user = userOpt.orElseGet(() -> User.builder()
                .googleId(payload.getGoogleId())
                .email(payload.getEmail())
                .name(payload.getName())
                .avatarUrl(payload.getPictureUrl())
                .city("Barwaha")
                .active(true)
                .build());

        if (user.getGoogleId() == null) {
            user.setGoogleId(payload.getGoogleId());
        }
        if (user.getAvatarUrl() == null && payload.getPictureUrl() != null) {
            user.setAvatarUrl(payload.getPictureUrl());
        }
        if (user.getName() == null && payload.getName() != null) {
            user.setName(payload.getName());
        }

        User savedUser = userRepository.save(user);
        return createLoginResponse(savedUser, isNewUser);
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.refreshToken().trim());

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid or revoked"));

        if (refreshToken.isExpired()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new UnauthorizedException("Refresh token has expired. Please log in again.");
        }

        User user = refreshToken.getUser();
        if (!user.isActive() || user.isDeleted()) {
            throw new UnauthorizedException("User account is inactive or disabled");
        }

        // Refresh Token Rotation: Revoke existing token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Issue fresh token pair
        return createLoginResponse(user, user.getRole() == null);
    }

    @Override
    @Transactional
    public void logout(UUID userId, LogoutRequest request) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().trim().isEmpty()) {
            String tokenHash = hashToken(request.refreshToken().trim());
            refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        } else if (userId != null) {
            refreshTokenRepository.revokeAllByUserId(userId);
        }
        log.info("User {} logged out successfully", userId);
    }

    private LoginResponse createLoginResponse(User user, boolean isNewUser) {
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole(), user.getPhone(), user.getEmail());
        String rawRefreshToken = jwtProvider.generateRefreshTokenString();

        Instant refreshTokenExpiry = Instant.now().plusMillis(jwtProvider.getRefreshTokenExpirationMs());
        RefreshToken tokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawRefreshToken))
                .expiresAt(refreshTokenExpiry)
                .revoked(false)
                .build();

        refreshTokenRepository.save(tokenEntity);

        return new LoginResponse(
                accessToken,
                rawRefreshToken,
                "Bearer",
                900, // 15 min
                isNewUser,
                user.getRole(),
                UserSummaryDto.fromEntity(user)
        );
    }

    private String normalizeTarget(String target) {
        if (target == null) return "";
        String trimmed = target.trim();
        if (isPhoneNumber(trimmed)) {
            // Strip spaces, dashes, parentheses
            String digitsOnly = trimmed.replaceAll("[^0-9+]", "");
            if (digitsOnly.startsWith("+91") && digitsOnly.length() == 13) {
                return digitsOnly.substring(3);
            }
            if (digitsOnly.startsWith("91") && digitsOnly.length() == 12) {
                return digitsOnly.substring(2);
            }
            return digitsOnly;
        }
        return trimmed.toLowerCase();
    }

    private boolean isPhoneNumber(String target) {
        return target.matches("^[+]?[0-9\\s\\-()]{10,15}$");
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}

