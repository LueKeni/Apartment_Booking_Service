package com.realestate.apartment_booking_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {

    private Long conversationId;

    @NotNull
    private Long recipientId;

    @NotNull
    private Long apartmentId;

    @NotBlank
    private String content;
}
