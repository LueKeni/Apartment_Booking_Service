package com.realestate.apartment_booking_service.controllers.api;

import com.realestate.apartment_booking_service.dto.ChatMessageDto;
import com.realestate.apartment_booking_service.dto.ConversationSummaryDto;
import com.realestate.apartment_booking_service.dto.SendMessageRequest;
import com.realestate.apartment_booking_service.entities.Conversation;
import com.realestate.apartment_booking_service.entities.Message;
import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.services.interfaces.ChatService;
import com.realestate.apartment_booking_service.services.interfaces.UserService;
import com.realestate.apartment_booking_service.utils.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatRestController {

    private final ChatService chatService;
    private final UserService userService;

    @GetMapping("/conversations")
    public List<ConversationSummaryDto> getConversations() {
        Long currentUserId = currentUser().getId();
        return chatService.getUserConversations(currentUserId)
                .stream()
                .map(conversation -> mapConversation(conversation, currentUserId))
                .toList();
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<ChatMessageDto> getMessages(@PathVariable Long conversationId) {
        Long currentUserId = currentUser().getId();
        return chatService.getConversationMessages(conversationId, currentUserId)
                .stream()
                .map(this::mapMessage)
                .toList();
    }

    @PostMapping("/messages")
    public ChatMessageDto sendMessage(@Valid @RequestBody SendMessageRequest request) {
        User user = currentUser();
        Message saved = chatService.sendMessage(
                user.getId(),
                request.getRecipientId(),
                request.getApartmentId(),
                request.getConversationId(),
                request.getContent());
        return mapMessage(saved);
    }

    @PostMapping("/conversations/{conversationId}/read")
    public void markRead(@PathVariable Long conversationId) {
        chatService.markConversationRead(conversationId, currentUser().getId());
    }

    @GetMapping("/unread-count")
    public long unreadCount() {
        return chatService.countUnreadMessages(currentUser().getId());
    }

    private ConversationSummaryDto mapConversation(Conversation conversation, Long currentUserId) {
        boolean selfIsUser = conversation.getUser().getId().equals(currentUserId);
        User other = selfIsUser ? conversation.getAgent() : conversation.getUser();

        return ConversationSummaryDto.builder()
                .conversationId(conversation.getId())
                .apartmentId(conversation.getApartment().getId())
                .apartmentTitle(conversation.getApartment().getTitle())
                .otherUserId(other.getId())
                .otherUserName(other.getFullName())
                .lastMessageAt(conversation.getLastMessageAt())
                .build();
    }

    private ChatMessageDto mapMessage(Message message) {
        return ChatMessageDto.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .readFlag(message.getReadFlag())
                .build();
    }

    private User currentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userService.findByEmail(email);
    }
}
