package com.realestate.apartment_booking_service.services.impl;

import com.realestate.apartment_booking_service.entities.Apartment;
import com.realestate.apartment_booking_service.entities.Conversation;
import com.realestate.apartment_booking_service.entities.Message;
import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.NotificationType;
import com.realestate.apartment_booking_service.enums.Role;
import com.realestate.apartment_booking_service.repositories.ApartmentRepository;
import com.realestate.apartment_booking_service.repositories.ConversationRepository;
import com.realestate.apartment_booking_service.repositories.MessageRepository;
import com.realestate.apartment_booking_service.repositories.UserRepository;
import com.realestate.apartment_booking_service.services.interfaces.ChatService;
import com.realestate.apartment_booking_service.services.interfaces.NotificationService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final NotificationService notificationService;

    @Override
    public Conversation getOrCreateConversation(Long senderId, Long recipientId, Long apartmentId) {
        User sender = getUser(senderId);
        User recipient = getUser(recipientId);
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Apartment not found"));

        User userParty = sender.getRole() == Role.AGENT ? recipient : sender;
        User agentParty = sender.getRole() == Role.AGENT ? sender : recipient;

        return conversationRepository
                .findByUserIdAndAgentIdAndApartmentId(userParty.getId(), agentParty.getId(), apartmentId)
                .orElseGet(() -> conversationRepository.save(
                        Conversation.builder()
                                .user(userParty)
                                .agent(agentParty)
                                .apartment(apartment)
                                .lastMessageAt(LocalDateTime.now())
                                .build()));
    }

    @Override
    public Message sendMessage(Long senderId, Long recipientId, Long apartmentId, Long conversationId, String content) {
        User sender = getUser(senderId);
        Conversation conversation = conversationId == null
                ? getOrCreateConversation(senderId, recipientId, apartmentId)
                : conversationRepository.findWithParticipantsById(conversationId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (!isParticipant(conversation, senderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this conversation");
        }

        Message message = messageRepository.save(
                Message.builder()
                        .conversation(conversation)
                        .sender(sender)
                        .content(content)
                        .sentAt(LocalDateTime.now())
                        .readFlag(false)
                        .build());

        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        Long notifyRecipientId = conversation.getUser().getId().equals(senderId)
                ? conversation.getAgent().getId()
                : conversation.getUser().getId();

        notificationService.createNotification(
                notifyRecipientId,
                "New message",
                sender.getFullName() + " sent you a message",
                NotificationType.CHAT);

        return message;
    }

    @Override
    public List<Message> getConversationMessages(Long conversationId, Long currentUserId) {
        Conversation conversation = conversationRepository.findWithParticipantsById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (!isParticipant(conversation, currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this conversation");
        }

        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
    }

    @Override
    public List<Conversation> getUserConversations(Long userId) {
        return conversationRepository.findByUserIdOrAgentIdOrderByLastMessageAtDesc(userId, userId);
    }

    @Override
    public void markConversationRead(Long conversationId, Long currentUserId) {
        Conversation conversation = conversationRepository.findWithParticipantsById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (!isParticipant(conversation, currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this conversation");
        }

        List<Message> messages = messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
        for (Message message : messages) {
            if (!message.getSender().getId().equals(currentUserId) && !Boolean.TRUE.equals(message.getReadFlag())) {
                message.setReadFlag(true);
            }
        }
        messageRepository.saveAll(messages);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private boolean isParticipant(Conversation conversation, Long userId) {
        return conversation.getUser().getId().equals(userId) || conversation.getAgent().getId().equals(userId);
    }
}
