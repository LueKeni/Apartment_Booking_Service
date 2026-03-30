package com.realestate.apartment_booking_service.services.interfaces;

import com.realestate.apartment_booking_service.dto.BookingRequest;
import com.realestate.apartment_booking_service.entities.Appointment;
import com.realestate.apartment_booking_service.enums.AppointmentStatus;
import java.util.List;

public interface BookingService {

    Appointment createBooking(Long userId, BookingRequest request);

    Appointment cancelBooking(Long userId, Long appointmentId);

    Appointment updateBookingStatus(Long agentId, Long appointmentId, AppointmentStatus status, String agentNote);

    List<Appointment> getUserAppointments(Long userId);

    List<Appointment> getAgentAppointments(Long agentId);
}
