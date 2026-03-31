package com.realestate.apartment_booking_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MomoExecuteResponseModel {

    private String orderId;
    private String amount;
    private String fullName;
    private String orderInfo;
    private String message;
    private String errorCode;
    private Long points;
    private String status;
}
