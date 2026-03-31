package com.realestate.apartment_booking_service.repositories;

import com.realestate.apartment_booking_service.entities.Review;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<Review> findByAgentIdOrderByCreatedAtDesc(Long agentId);

    @EntityGraph(attributePaths = { "appointment" })
    List<Review> findByUserId(Long userId);

    boolean existsByAppointmentIdAndUserId(Long appointmentId, Long userId);

    long deleteByAppointmentApartmentId(Long apartmentId);
}
