package com.realestate.apartment_booking_service.repositories;

import com.realestate.apartment_booking_service.entities.AgentProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentProfileRepository extends JpaRepository<AgentProfile, Long> {

    Optional<AgentProfile> findByUserId(Long userId);
}
