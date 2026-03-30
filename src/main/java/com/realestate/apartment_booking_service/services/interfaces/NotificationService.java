package com.realestate.apartment_booking_service.services.interfaces;

import com.realestate.apartment_booking_service.dto.NotificationDto;
import com.realestate.apartment_booking_service.enums.NotificationType;
import java.util.List;

public interface NotificationService {

    void createNotification(Long recipientId, String title, String message, NotificationType type);

    List<NotificationDto> getNotifications(Long recipientId);

    long countUnread(Long recipientId);

    void markRead(Long notificationId, Long recipientId);

    void markAllRead(Long recipientId);
}
