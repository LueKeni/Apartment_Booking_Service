package com.realestate.apartment_booking_service.repositories;

import com.realestate.apartment_booking_service.entities.Conversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @EntityGraph(attributePaths = { "user", "agent", "apartment" })
    Optional<Conversation> findByUserIdAndAgentIdAndApartmentId(Long userId, Long agentId, Long apartmentId);

    @EntityGraph(attributePaths = { "user", "agent", "apartment" })
    List<Conversation> findByUserIdOrAgentIdOrderByLastMessageAtDesc(Long userId, Long agentId);

    @EntityGraph(attributePaths = { "user", "agent", "apartment" })
    Optional<Conversation> findWithParticipantsById(Long id);
}
