package com.realestate.apartment_booking_service.dto;

import lombok.Builder;

@Builder
public record ApartmentAssistantResultDto(
        Long id,
        String title,
        String priceLabel,
        String district,
        String roomType,
        String detailUrl,
        String imageUrl) {
}
