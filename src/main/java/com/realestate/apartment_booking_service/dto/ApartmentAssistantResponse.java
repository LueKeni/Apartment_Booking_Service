package com.realestate.apartment_booking_service.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record ApartmentAssistantResponse(
        String answer,
        List<String> appliedFilters,
        List<ApartmentAssistantResultDto> suggestions) {
}
