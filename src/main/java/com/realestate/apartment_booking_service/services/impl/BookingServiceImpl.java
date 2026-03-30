package com.realestate.apartment_booking_service.services.impl;

import com.realestate.apartment_booking_service.dto.BookingRequest;
import com.realestate.apartment_booking_service.entities.Apartment;
import com.realestate.apartment_booking_service.entities.Appointment;
import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.AppointmentStatus;
import com.realestate.apartment_booking_service.enums.NotificationType;
import com.realestate.apartment_booking_service.repositories.ApartmentRepository;
import com.realestate.apartment_booking_service.repositories.AppointmentRepository;
import com.realestate.apartment_booking_service.repositories.UserRepository;
import com.realestate.apartment_booking_service.services.interfaces.BookingService;
import com.realestate.apartment_booking_service.services.interfaces.NotificationService;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

    private final AppointmentRepository appointmentRepository;
    private final ApartmentRepository apartmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    public Appointment createBooking(Long userId, BookingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Apartment apartment = apartmentRepository.findById(request.getApartmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Apartment not found"));

        boolean conflict = appointmentRepository.existsByApartmentIdAndScheduledDateAndScheduledTimeAndStatusIn(
                apartment.getId(),
                request.getScheduledDate(),
                request.getScheduledTime(),
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED));

        if (conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This slot is already booked");
        }

        Appointment appointment = Appointment.builder()
                .user(user)
                .agent(apartment.getAgent())
                .apartment(apartment)
                .scheduledDate(request.getScheduledDate())
                .scheduledTime(request.getScheduledTime())
                .status(AppointmentStatus.PENDING)
                .userNote(request.getUserNote())
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        notificationService.createNotification(
                apartment.getAgent().getId(),
                "New booking request",
                user.getFullName() + " booked a viewing for " + apartment.getTitle(),
                NotificationType.BOOKING);

        return saved;
    }

    @Override
    public Appointment cancelBooking(Long userId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (!appointment.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this appointment");
        }

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending appointments can be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);

        notificationService.createNotification(
                appointment.getAgent().getId(),
                "Booking cancelled",
                appointment.getUser().getFullName() + " cancelled an appointment for "
                        + appointment.getApartment().getTitle(),
                NotificationType.BOOKING);

        return saved;
    }

    @Override
    public Appointment updateBookingStatus(Long agentId, Long appointmentId, AppointmentStatus status,
            String agentNote) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (!appointment.getAgent().getId().equals(agentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this appointment");
        }

        appointment.setStatus(status);
        appointment.setAgentNote(agentNote);
        Appointment saved = appointmentRepository.save(appointment);

        notificationService.createNotification(
                appointment.getUser().getId(),
                "Appointment updated",
                "Your appointment for " + appointment.getApartment().getTitle() + " is now " + status.name(),
                NotificationType.BOOKING);

        return saved;
    }

    @Override
    public List<Appointment> getUserAppointments(Long userId) {
        return appointmentRepository.findByUserIdOrderByScheduledDateDescScheduledTimeDesc(userId);
    }

    @Override
    public List<Appointment> getAgentAppointments(Long agentId) {
        return appointmentRepository.findByAgentIdOrderByScheduledDateDescScheduledTimeDesc(agentId);
    }
}
