package com.realestate.apartment_booking_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "momo")
public class MomoOptionModel {

    private String apiUrl;
    private String secretKey;
    private String accessKey;
    private String returnUrl;
    private String notifyUrl;
    private String partnerCode;
    private String requestType;
}
