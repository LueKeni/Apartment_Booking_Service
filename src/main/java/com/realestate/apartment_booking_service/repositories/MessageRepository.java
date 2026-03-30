package com.realestate.apartment_booking_service.repositories;

import com.realestate.apartment_booking_service.entities.Message;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @EntityGraph(attributePaths = { "sender", "conversation" })
    List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);
}
