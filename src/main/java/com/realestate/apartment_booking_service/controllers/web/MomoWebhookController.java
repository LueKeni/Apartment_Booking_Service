package com.realestate.apartment_booking_service.controllers.web;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class MomoWebhookController {

    @PostMapping("/momo/notify")
    public ResponseEntity<String> momoNotify(@RequestBody Map<String, Object> payload) {
        log.info("MoMo notify payload: {}", payload);
        return ResponseEntity.ok("OK");
    }
}
