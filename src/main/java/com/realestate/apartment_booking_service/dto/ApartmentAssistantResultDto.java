package com.realestate.apartment_booking_service.dto;

import lombok.Builder;

@Builder
public record ApartmentAssistantResultDto(
        Long id,
        String title,
        String priceLabel,
        String district,
        String roomType,
        Integer bedrooms,
        Integer bathrooms,
        String detailUrl,
        String imageUrl) {
}
