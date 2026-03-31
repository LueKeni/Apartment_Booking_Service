package com.realestate.apartment_booking_service.services.interfaces;

import com.realestate.apartment_booking_service.dto.NotificationDto;
import com.realestate.apartment_booking_service.enums.NotificationType;
import java.util.List;

public interface NotificationService {

    default void createNotification(Long recipientId, String title, String message, NotificationType type) {
        createNotification(recipientId, title, message, type, null);
    }

    void createNotification(Long recipientId, String title, String message, NotificationType type, String actionUrl);

    List<NotificationDto> getNotifications(Long recipientId);

    long countUnread(Long recipientId);

    void markRead(Long notificationId, Long recipientId);

    void markAllRead(Long recipientId);

    String markReadAndResolveActionUrl(Long notificationId, Long recipientId);
}
