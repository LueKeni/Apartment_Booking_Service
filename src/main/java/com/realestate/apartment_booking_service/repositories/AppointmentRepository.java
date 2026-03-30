package com.realestate.apartment_booking_service.repositories;

import com.realestate.apartment_booking_service.entities.Appointment;
import com.realestate.apartment_booking_service.enums.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @EntityGraph(attributePaths = { "apartment", "user", "agent" })
    List<Appointment> findByUserIdOrderByScheduledDateDescScheduledTimeDesc(Long userId);

    @EntityGraph(attributePaths = { "apartment", "user", "agent" })
    List<Appointment> findByAgentIdOrderByScheduledDateDescScheduledTimeDesc(Long agentId);

    boolean existsByApartmentIdAndScheduledDateAndScheduledTimeAndStatusIn(
            Long apartmentId,
            LocalDate scheduledDate,
            LocalTime scheduledTime,
            Collection<AppointmentStatus> statuses);
}
