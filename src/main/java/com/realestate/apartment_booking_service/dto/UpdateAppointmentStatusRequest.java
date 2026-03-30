package com.realestate.apartment_booking_service.dto;

import com.realestate.apartment_booking_service.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAppointmentStatusRequest {

    @NotNull
    private AppointmentStatus status;

    private String agentNote;
}
