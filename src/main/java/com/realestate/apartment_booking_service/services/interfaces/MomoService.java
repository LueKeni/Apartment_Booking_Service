package com.realestate.apartment_booking_service.services.interfaces;

import com.realestate.apartment_booking_service.dto.MomoCreatePaymentResponseModel;
import com.realestate.apartment_booking_service.dto.MomoExecuteResponseModel;
import com.realestate.apartment_booking_service.dto.MomoOrderInfoRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface MomoService {

    MomoCreatePaymentResponseModel createPayment(MomoOrderInfoRequest request, Long userId);

    MomoExecuteResponseModel handleCallback(HttpServletRequest request);
}
