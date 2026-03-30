package com.realestate.apartment_booking_service.repositories;

import com.realestate.apartment_booking_service.entities.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);
}
