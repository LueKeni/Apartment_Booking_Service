package com.realestate.apartment_booking_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MomoCreatePaymentResponseModel {

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("errorCode")
    private Integer errorCode;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("message")
    private String message;

    @JsonProperty("localMessage")
    private String localMessage;

    @JsonProperty("requestType")
    private String requestType;

    @JsonProperty("payUrl")
    private String payUrl;

    @JsonProperty("signature")
    private String signature;

    @JsonProperty("qrCodeUrl")
    private String qrCodeUrl;

    @JsonProperty("deeplink")
    private String deeplink;

    @JsonProperty("deeplinkWebInApp")
    private String deeplinkWebInApp;
}
