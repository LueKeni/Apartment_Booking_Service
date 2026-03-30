package com.realestate.apartment_booking_service.dto;

import lombok.Data;

@Data
public class SocketChatMessage {

    private Long senderId;
    private Long recipientId;
    private Long apartmentId;
    private Long conversationId;
    private String content;
}
