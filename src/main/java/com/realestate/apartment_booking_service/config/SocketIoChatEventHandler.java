package com.realestate.apartment_booking_service.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.realestate.apartment_booking_service.dto.SocketChatMessage;
import com.realestate.apartment_booking_service.entities.Message;
import com.realestate.apartment_booking_service.services.interfaces.ChatService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocketIoChatEventHandler {

    private final SocketIOServer socketIOServer;
    private final ChatService chatService;

    @PostConstruct
    public void registerListeners() {
        socketIOServer.addEventListener("send_message", SocketChatMessage.class, (client, data, ackSender) -> {
            Message saved = chatService.sendMessage(
                    data.getSenderId(),
                    data.getRecipientId(),
                    data.getApartmentId(),
                    data.getConversationId(),
                    data.getContent());

            socketIOServer.getBroadcastOperations().sendEvent("receive_message", saved);
        });
    }
}
