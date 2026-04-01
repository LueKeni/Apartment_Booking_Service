package com.realestate.apartment_booking_service.repositories;

import com.realestate.apartment_booking_service.entities.AgentProfile;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AgentProfileRepository extends JpaRepository<AgentProfile, Long> {

    @EntityGraph(attributePaths = {"user"})
    Optional<AgentProfile> findByUserId(Long userId);

    @Query("SELECT ap FROM AgentProfile ap JOIN FETCH ap.user u WHERE ap.reviewCount > 0 ORDER BY ap.averageRating DESC, ap.reviewCount DESC")
    List<AgentProfile> findTopAgents(Pageable pageable);
}
