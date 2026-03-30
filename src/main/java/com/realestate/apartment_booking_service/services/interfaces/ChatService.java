package com.realestate.apartment_booking_service.services.interfaces;

import com.realestate.apartment_booking_service.entities.Conversation;
import com.realestate.apartment_booking_service.entities.Message;
import java.util.List;

public interface ChatService {

    Conversation getOrCreateConversation(Long senderId, Long recipientId, Long apartmentId);

    Message sendMessage(Long senderId, Long recipientId, Long apartmentId, Long conversationId, String content);

    List<Message> getConversationMessages(Long conversationId, Long currentUserId);

    List<Conversation> getUserConversations(Long userId);

    void markConversationRead(Long conversationId, Long currentUserId);
}
