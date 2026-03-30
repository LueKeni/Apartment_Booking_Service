package com.realestate.apartment_booking_service.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;

@Data
public class BookingRequest {

    @NotNull
    private Long apartmentId;

    @NotNull
    @FutureOrPresent
    private LocalDate scheduledDate;

    @NotNull
    private LocalTime scheduledTime;

    private String userNote;
}
