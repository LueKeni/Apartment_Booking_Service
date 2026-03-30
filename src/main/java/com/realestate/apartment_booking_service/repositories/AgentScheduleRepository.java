package com.realestate.apartment_booking_service.repositories;

import com.realestate.apartment_booking_service.entities.AgentSchedule;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentScheduleRepository extends JpaRepository<AgentSchedule, Long> {

    Optional<AgentSchedule> findByAgentIdAndAvailableDate(Long agentId, LocalDate availableDate);

    List<AgentSchedule> findByAgentIdAndAvailableDateGreaterThanEqualOrderByAvailableDateAsc(Long agentId,
            LocalDate availableDate);
}
