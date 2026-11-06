package com.project.rentmebarwaha.notification.service;

import com.project.rentmebarwaha.notification.dto.GroupedNotificationsResponse;
import com.project.rentmebarwaha.notification.dto.RegisterDeviceTokenRequest;
import com.project.rentmebarwaha.notification.dto.UnreadCountResponse;
import com.project.rentmebarwaha.notification.entity.NotificationType;

import java.util.UUID;

public interface NotificationService {

    void sendNotification(UUID userId, NotificationType type, String title, String body, String refType, String refId);

    GroupedNotificationsResponse getNotifications(UUID userId, String typeFilter, int page, int size);

    UnreadCountResponse getUnreadCount(UUID userId);

    void markAsRead(UUID userId, UUID notificationId);

    void markAllAsRead(UUID userId);

    void registerDevice(UUID userId, RegisterDeviceTokenRequest request);
}

