package com.project.rentmebarwaha.auth.service;

import com.project.rentmebarwaha.auth.dto.*;

import java.util.UUID;

public interface AuthService {

    OtpSendResponse sendOtp(OtpSendRequest request);

    LoginResponse verifyOtp(OtpVerifyRequest request);

    LoginResponse googleLogin(GoogleLoginRequest request);

    LoginResponse refreshToken(RefreshTokenRequest request);

    void logout(UUID userId, LogoutRequest request);
}

