package com.realestate.apartment_booking_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MomoOrderInfoRequest {

    @NotBlank
    private String fullName;

    @Min(1000)
    private long amount;

    @NotBlank
    private String orderInfo;

    private Long points;

    private String orderId;
}
