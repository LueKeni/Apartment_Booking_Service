package com.realestate.apartment_booking_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApartmentAssistantRequest {

    @NotBlank
    private String message;
}
