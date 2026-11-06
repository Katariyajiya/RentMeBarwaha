package com.project.rentmebarwaha.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@ConditionalOnProperty(name = "nestearth.messaging.provider", havingValue = "log", matchIfMissing = true)
public class LoggingPushSender implements PushSender {

    @Override
    public void sendPush(String fcmToken, String title, String body, Map<String, String> data) {
        log.info("=================================================");
        log.info("[PUSH NOTIFICATION] To Token: {}", fcmToken);
        log.info("Title: {}", title);
        log.info("Body: {}", body);
        log.info("Data payload: {}", data);
        log.info("=================================================");
    }
}

