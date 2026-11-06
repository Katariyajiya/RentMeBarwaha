package com.project.rentmebarwaha.auth.service;

public interface OtpSender {
    void sendOtp(String target, String code);
}

