package com.realestate.apartment_booking_service.controllers.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ChatController {

    @GetMapping("/chat")
    public String chat(
            @RequestParam(required = false) Long conversationId,
            @RequestParam(required = false) Long recipientId,
            @RequestParam(required = false) Long apartmentId,
            Model model) {
        model.addAttribute("initialConversationId", conversationId);
        model.addAttribute("initialRecipientId", recipientId);
        model.addAttribute("initialApartmentId", apartmentId);
        return "common/chat";
    }
}
