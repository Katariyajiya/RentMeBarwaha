package com.project.rentmebarwaha.notification.service;

import java.util.Map;

public interface PushSender {

    void sendPush(String fcmToken, String title, String body, Map<String, String> data);
}

