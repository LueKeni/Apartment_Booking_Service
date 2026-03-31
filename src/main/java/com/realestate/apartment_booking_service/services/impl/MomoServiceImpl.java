package com.realestate.apartment_booking_service.services.impl;

import com.realestate.apartment_booking_service.config.MomoOptionModel;
import com.realestate.apartment_booking_service.dto.MomoCreatePaymentResponseModel;
import com.realestate.apartment_booking_service.dto.MomoExecuteResponseModel;
import com.realestate.apartment_booking_service.dto.MomoOrderInfoRequest;
import com.realestate.apartment_booking_service.entities.PointTopUp;
import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.NotificationType;
import com.realestate.apartment_booking_service.enums.PaymentStatus;
import com.realestate.apartment_booking_service.repositories.PointTopUpRepository;
import com.realestate.apartment_booking_service.repositories.UserRepository;
import com.realestate.apartment_booking_service.services.interfaces.MomoService;
import com.realestate.apartment_booking_service.services.interfaces.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Transactional
public class MomoServiceImpl implements MomoService {

    private final MomoOptionModel options;
    private final PointTopUpRepository pointTopUpRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public MomoCreatePaymentResponseModel createPayment(MomoOrderInfoRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        String orderId = request.getOrderId();
        if (orderId == null || orderId.isBlank()) {
            orderId = String.valueOf(Instant.now().toEpochMilli());
        }

        long points = resolvePoints(request);
        String orderInfo = "Khach hang: " + request.getFullName() + ". Noi dung: " + request.getOrderInfo();
        String rawData = "partnerCode=" + options.getPartnerCode()
                + "&accessKey=" + options.getAccessKey()
                + "&requestId=" + orderId
                + "&amount=" + request.getAmount()
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&returnUrl=" + options.getReturnUrl()
                + "&notifyUrl=" + options.getNotifyUrl()
                + "&extraData=";

        String signature = computeHmacSha256(rawData, options.getSecretKey());

        Map<String, Object> requestData = new LinkedHashMap<>();
        requestData.put("accessKey", options.getAccessKey());
        requestData.put("partnerCode", options.getPartnerCode());
        requestData.put("requestType", options.getRequestType());
        requestData.put("notifyUrl", options.getNotifyUrl());
        requestData.put("returnUrl", options.getReturnUrl());
        requestData.put("orderId", orderId);
        requestData.put("amount", String.valueOf(request.getAmount()));
        requestData.put("orderInfo", orderInfo);
        requestData.put("requestId", orderId);
        requestData.put("extraData", "");
        requestData.put("signature", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestData, headers);

        PointTopUp topUp = PointTopUp.builder()
                .user(user)
                .orderId(orderId)
                .amount(request.getAmount())
                .points(points)
                .orderInfo(orderInfo)
                .status(PaymentStatus.PENDING)
                .build();
        pointTopUpRepository.save(topUp);

        ResponseEntity<MomoCreatePaymentResponseModel> response = restTemplate.postForEntity(options.getApiUrl(),
                entity, MomoCreatePaymentResponseModel.class);
        MomoCreatePaymentResponseModel body = response.getBody();
        if (body == null || body.getPayUrl() == null || body.getPayUrl().isBlank()) {
            topUp.setStatus(PaymentStatus.FAILED);
            topUp.setMessage("MoMo response missing payUrl");
            pointTopUpRepository.save(topUp);
        } else if (body.getErrorCode() != null && body.getErrorCode() != 0) {
            topUp.setStatus(PaymentStatus.FAILED);
            topUp.setErrorCode(String.valueOf(body.getErrorCode()));
            topUp.setMessage(body.getMessage());
            pointTopUpRepository.save(topUp);
        }
        return body;
    }

    @Override
    public MomoExecuteResponseModel handleCallback(HttpServletRequest request) {
        String amount = request.getParameter("amount");
        String orderInfo = request.getParameter("orderInfo");
        String orderId = request.getParameter("orderId");
        String message = request.getParameter("message");
        String errorCode = request.getParameter("errorCode");
        String resultCode = request.getParameter("resultCode");
        String statusCode = errorCode != null ? errorCode : resultCode;
        boolean success = "0".equals(statusCode);

        final long[] pointsHolder = new long[] { 0L };
        if (orderId != null && !orderId.isBlank()) {
            pointTopUpRepository.findByOrderId(orderId).ifPresent(topUp -> {
                topUp.setMessage(message);
                topUp.setErrorCode(statusCode);
                pointsHolder[0] = topUp.getPoints();
                if (success && topUp.getStatus() != PaymentStatus.SUCCESS) {
                    topUp.setStatus(PaymentStatus.SUCCESS);
                    User user = topUp.getUser();
                    if (user.getPoints() == null) {
                        user.setPoints(0L);
                    }
                    user.setPoints(user.getPoints() + topUp.getPoints());
                    userRepository.save(user);

                    notificationService.createNotification(
                            user.getId(),
                            "Top-up successful",
                            "You received " + topUp.getPoints() + " points from order " + topUp.getOrderId() + ".",
                            NotificationType.POINTS,
                            "/agent/points");
                } else if (!success) {
                    topUp.setStatus(PaymentStatus.FAILED);
                }
                pointTopUpRepository.save(topUp);
            });
        }

        return MomoExecuteResponseModel.builder()
                .amount(amount)
                .orderId(orderId)
                .orderInfo(orderInfo)
                .fullName(request.getParameter("fullName"))
                .message(message)
                .errorCode(statusCode)
                .points(pointsHolder[0] > 0 ? pointsHolder[0] : null)
                .status(success ? "SUCCESS" : "FAILED")
                .build();
    }

    private long resolvePoints(MomoOrderInfoRequest request) {
        if (request.getPoints() != null && request.getPoints() > 0) {
            return request.getPoints();
        }
        long computed = request.getAmount() / 100;
        return Math.max(1, computed);
    }

    private String computeHmacSha256(String message, String secretKey) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(keySpec);
            byte[] hashBytes = hmac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign Momo request", ex);
        }
    }
}
