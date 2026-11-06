package com.project.rentmebarwaha.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(name = "nestearth.messaging.provider", havingValue = "log", matchIfMissing = true)
public class LoggingOtpSender implements OtpSender {

    @Override
    public void sendOtp(String target, String code) {
        log.info("=================================================");
        log.info("[OTP SERVICE] Dispatched OTP code [{}] to target [{}]", code, target);
        log.info("=================================================");
    }
}

