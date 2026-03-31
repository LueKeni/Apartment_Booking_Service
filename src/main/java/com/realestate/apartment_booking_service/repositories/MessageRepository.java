package com.realestate.apartment_booking_service.repositories;

import com.realestate.apartment_booking_service.entities.Message;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @EntityGraph(attributePaths = { "sender", "conversation" })
    List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);

    @Query("""
            select count(m)
            from Message m
            join m.conversation c
            where m.readFlag = false
              and m.sender.id <> :userId
              and (c.user.id = :userId or c.agent.id = :userId)
            """)
    long countUnreadForUser(@Param("userId") Long userId);
}
