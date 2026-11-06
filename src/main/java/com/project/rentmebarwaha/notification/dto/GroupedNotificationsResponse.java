package com.project.rentmebarwaha.notification.dto;

import java.util.List;

public record GroupedNotificationsResponse(
        List<NotificationResponse> today,
        List<NotificationResponse> yesterday,
        List<NotificationResponse> earlier,
        long unreadCount,
        int page,
        int size,
        long totalElements
) {}

