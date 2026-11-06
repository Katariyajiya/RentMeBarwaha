package com.project.rentmebarwaha.notification.service;

import com.project.rentmebarwaha.common.exception.ResourceNotFoundException;
import com.project.rentmebarwaha.notification.dto.GroupedNotificationsResponse;
import com.project.rentmebarwaha.notification.dto.NotificationResponse;
import com.project.rentmebarwaha.notification.dto.RegisterDeviceTokenRequest;
import com.project.rentmebarwaha.notification.dto.UnreadCountResponse;
import com.project.rentmebarwaha.notification.entity.DeviceToken;
import com.project.rentmebarwaha.notification.entity.Notification;
import com.project.rentmebarwaha.notification.entity.NotificationType;
import com.project.rentmebarwaha.notification.repository.DeviceTokenRepository;
import com.project.rentmebarwaha.notification.repository.NotificationRepository;
import com.project.rentmebarwaha.user.entity.User;
import com.project.rentmebarwaha.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final PushSender pushSender;

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");

    @Override
    @Transactional
    public void sendNotification(UUID userId, NotificationType type, String title, String body, String refType, String refId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElse(null);
        if (user == null) {
            log.warn("Cannot send notification to missing or deleted user: {}", userId);
            return;
        }

        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .referenceType(refType)
                .referenceId(refId)
                .read(false)
                .build();

        notificationRepository.save(notification);

        // Dispatch push notification to all registered user devices
        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(userId);
        if (!tokens.isEmpty()) {
            Map<String, String> data = new HashMap<>();
            data.put("notificationId", notification.getId().toString());
            data.put("type", type.name());
            if (refType != null) data.put("referenceType", refType);
            if (refId != null) data.put("referenceId", refId);

            for (DeviceToken token : tokens) {
                try {
                    pushSender.sendPush(token.getFcmToken(), title, body, data);
                } catch (Exception e) {
                    log.error("Failed to dispatch push notification to device {}: {}", token.getId(), e.getMessage());
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GroupedNotificationsResponse getNotifications(UUID userId, String typeFilter, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));

        Page<Notification> notificationPage;
        if (typeFilter != null && !typeFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(typeFilter)) {
            try {
                NotificationType type = NotificationType.valueOf(typeFilter.trim().toUpperCase());
                notificationPage = notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
            } catch (IllegalArgumentException e) {
                notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
            }
        } else {
            notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        // Group into Today, Yesterday, Earlier
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        Instant startOfToday = today.atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant startOfYesterday = today.minusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();

        List<NotificationResponse> todayList = new ArrayList<>();
        List<NotificationResponse> yesterdayList = new ArrayList<>();
        List<NotificationResponse> earlierList = new ArrayList<>();

        for (Notification n : notificationPage.getContent()) {
            NotificationResponse resp = NotificationResponse.fromEntity(n);
            Instant created = n.getCreatedAt();
            if (created.isAfter(startOfToday)) {
                todayList.add(resp);
            } else if (created.isAfter(startOfYesterday)) {
                yesterdayList.add(resp);
            } else {
                earlierList.add(resp);
            }
        }

        long unreadCount = notificationRepository.countByUserIdAndReadFalse(userId);

        return new GroupedNotificationsResponse(
                todayList,
                yesterdayList,
                earlierList,
                unreadCount,
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID userId) {
        long count = notificationRepository.countByUserIdAndReadFalse(userId);
        return new UnreadCountResponse(count);
    }

    @Override
    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (notification.getUser().getId().equals(userId)) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllReadForUser(userId);
    }

    @Override
    @Transactional
    public void registerDevice(UUID userId, RegisterDeviceTokenRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Optional<DeviceToken> existing = deviceTokenRepository.findByFcmToken(request.fcmToken().trim());
        if (existing.isPresent()) {
            DeviceToken token = existing.get();
            token.setUser(user);
            if (request.deviceType() != null) {
                token.setDeviceType(request.deviceType());
            }
            deviceTokenRepository.save(token);
        } else {
            DeviceToken newToken = DeviceToken.builder()
                    .user(user)
                    .fcmToken(request.fcmToken().trim())
                    .deviceType(request.deviceType() != null ? request.deviceType() : "FLUTTER_MOBILE")
                    .build();
            deviceTokenRepository.save(newToken);
        }
        log.info("Registered FCM device token for user {}", userId);
    }
}

