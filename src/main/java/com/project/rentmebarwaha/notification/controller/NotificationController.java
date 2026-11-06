package com.project.rentmebarwaha.notification.controller;

import com.project.rentmebarwaha.common.model.ApiResponse;
import com.project.rentmebarwaha.common.util.SecurityUtils;
import com.project.rentmebarwaha.notification.dto.GroupedNotificationsResponse;
import com.project.rentmebarwaha.notification.dto.RegisterDeviceTokenRequest;
import com.project.rentmebarwaha.notification.dto.UnreadCountResponse;
import com.project.rentmebarwaha.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Endpoints for fetching notifications, unread badges, marking read, and registering FCM push tokens")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get Notifications", description = "Retrieves grouped notifications (Today, Yesterday, Earlier) with optional type filter (ALL, INQUIRY, VISIT, SYSTEM, INSIGHT).")
    public ResponseEntity<ApiResponse<GroupedNotificationsResponse>> getNotifications(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GroupedNotificationsResponse response = notificationService.getNotifications(userId, type, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get Unread Count", description = "Returns the count of unread notifications for badge counters.")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount() {
        UUID userId = SecurityUtils.getCurrentUserId();
        UnreadCountResponse response = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark Notification as Read", description = "Marks a specific notification as read.")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        notificationService.markAsRead(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read"));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark All Notifications as Read", description = "Marks all unread notifications for the authenticated user as read.")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        UUID userId = SecurityUtils.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read"));
    }

    @PostMapping("/device-token")
    @Operation(summary = "Register FCM Device Token", description = "Registers or updates the Firebase Cloud Messaging device token for push notifications.")
    public ResponseEntity<ApiResponse<Void>> registerDeviceToken(@Valid @RequestBody RegisterDeviceTokenRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        notificationService.registerDevice(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Device token registered successfully"));
    }
}

