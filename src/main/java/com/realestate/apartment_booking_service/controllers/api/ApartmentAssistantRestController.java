package com.realestate.apartment_booking_service.controllers.api;

import com.realestate.apartment_booking_service.dto.ApartmentAssistantRequest;
import com.realestate.apartment_booking_service.dto.ApartmentAssistantResponse;
import com.realestate.apartment_booking_service.services.interfaces.ApartmentAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/assistant/apartments")
public class ApartmentAssistantRestController {

    private final ApartmentAssistantService apartmentAssistantService;

    @PostMapping
    public ApartmentAssistantResponse answer(@Valid @RequestBody ApartmentAssistantRequest request) {
        return apartmentAssistantService.answer(request.getMessage());
    }
}
