package com.project.rentmebarwaha.notification.dto;

import com.project.rentmebarwaha.notification.entity.Notification;
import com.project.rentmebarwaha.notification.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String body,
        String referenceType,
        String referenceId,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse fromEntity(Notification n) {
        if (n == null) return null;
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getReferenceType(),
                n.getReferenceId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}

