package com.project.rentmebarwaha.notification;

import com.project.rentmebarwaha.notification.dto.GroupedNotificationsResponse;
import com.project.rentmebarwaha.notification.dto.RegisterDeviceTokenRequest;
import com.project.rentmebarwaha.notification.dto.UnreadCountResponse;
import com.project.rentmebarwaha.notification.entity.DeviceToken;
import com.project.rentmebarwaha.notification.entity.Notification;
import com.project.rentmebarwaha.notification.entity.NotificationType;
import com.project.rentmebarwaha.notification.repository.DeviceTokenRepository;
import com.project.rentmebarwaha.notification.repository.NotificationRepository;
import com.project.rentmebarwaha.notification.service.NotificationServiceImpl;
import com.project.rentmebarwaha.notification.service.PushSender;
import com.project.rentmebarwaha.user.entity.User;
import com.project.rentmebarwaha.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PushSender pushSender;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .name("Rohit Sharma")
                .phone("9876543210")
                .build();
    }

    @Test
    void testSendNotification_Success() {
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        DeviceToken token = DeviceToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .fcmToken("fcm-token-123")
                .build();
        when(deviceTokenRepository.findByUserId(userId)).thenReturn(List.of(token));

        notificationService.sendNotification(userId, NotificationType.INQUIRY, "New Enquiry", "You have an enquiry", "ENQUIRY", "123");

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(pushSender, times(1)).sendPush(eq("fcm-token-123"), eq("New Enquiry"), eq("You have an enquiry"), anyMap());
    }

    @Test
    void testGetNotifications_GroupedResponse() {
        Notification n1 = Notification.builder()
                .id(UUID.randomUUID())
                .user(user)
                .type(NotificationType.INQUIRY)
                .title("Today Notif")
                .body("Body")
                .read(false)
                .build();
        n1.setCreatedAt(Instant.now());

        Notification n2 = Notification.builder()
                .id(UUID.randomUUID())
                .user(user)
                .type(NotificationType.VISIT)
                .title("Yesterday Notif")
                .body("Body")
                .read(true)
                .build();
        n2.setCreatedAt(Instant.now().minus(26, ChronoUnit.HOURS));

        Notification n3 = Notification.builder()
                .id(UUID.randomUUID())
                .user(user)
                .type(NotificationType.SYSTEM)
                .title("Earlier Notif")
                .body("Body")
                .read(false)
                .build();
        n3.setCreatedAt(Instant.now().minus(5, ChronoUnit.DAYS));

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(n1, n2, n3)));
        when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(2L);

        GroupedNotificationsResponse response = notificationService.getNotifications(userId, "ALL", 0, 20);

        assertNotNull(response);
        assertEquals(2L, response.unreadCount());
        assertEquals(3, response.totalElements());
    }

    @Test
    void testGetUnreadCount() {
        when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(5L);

        UnreadCountResponse response = notificationService.getUnreadCount(userId);

        assertEquals(5L, response.unreadCount());
    }

    @Test
    void testMarkAsRead() {
        UUID notifId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notifId)
                .user(user)
                .read(false)
                .build();

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(userId, notifId);

        assertTrue(notification.isRead());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void testMarkAllAsRead() {
        notificationService.markAllAsRead(userId);

        verify(notificationRepository, times(1)).markAllReadForUser(userId);
    }

    @Test
    void testRegisterDevice_NewToken() {
        RegisterDeviceTokenRequest request = new RegisterDeviceTokenRequest("new-fcm-token", "ANDROID");
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(deviceTokenRepository.findByFcmToken("new-fcm-token")).thenReturn(Optional.empty());

        notificationService.registerDevice(userId, request);

        verify(deviceTokenRepository, times(1)).save(any(DeviceToken.class));
    }

    @Test
    void testRegisterDevice_ExistingTokenUpdate() {
        RegisterDeviceTokenRequest request = new RegisterDeviceTokenRequest("existing-fcm-token", "IOS");
        DeviceToken existing = DeviceToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .fcmToken("existing-fcm-token")
                .deviceType("ANDROID")
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(deviceTokenRepository.findByFcmToken("existing-fcm-token")).thenReturn(Optional.of(existing));

        notificationService.registerDevice(userId, request);

        assertEquals("IOS", existing.getDeviceType());
        verify(deviceTokenRepository, times(1)).save(existing);
    }
}
