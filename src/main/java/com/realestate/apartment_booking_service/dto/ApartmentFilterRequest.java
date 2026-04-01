package com.realestate.apartment_booking_service.dto;

import com.realestate.apartment_booking_service.enums.ApartmentStatus;
import com.realestate.apartment_booking_service.enums.TransactionType;
import lombok.Data;

@Data
public class ApartmentFilterRequest {

    private String keyword;
    private String district;
    private String roomType;
    private Integer bedrooms;
    private TransactionType transactionType;
    private ApartmentStatus status;
}
