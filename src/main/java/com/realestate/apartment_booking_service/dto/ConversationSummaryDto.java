package com.realestate.apartment_booking_service.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConversationSummaryDto {

    private Long conversationId;
    private Long apartmentId;
    private String apartmentTitle;
    private Long otherUserId;
    private String otherUserName;
    private LocalDateTime lastMessageAt;
}
