package com.realestate.apartment_booking_service.dto;

import com.realestate.apartment_booking_service.enums.NotificationType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationDto {

    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
