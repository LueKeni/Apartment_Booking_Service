package com.realestate.apartment_booking_service.controllers.api;

import com.realestate.apartment_booking_service.dto.NotificationDto;
import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.services.interfaces.NotificationService;
import com.realestate.apartment_booking_service.services.interfaces.UserService;
import com.realestate.apartment_booking_service.utils.SecurityUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationRestController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public List<NotificationDto> getNotifications() {
        return notificationService.getNotifications(currentUser().getId());
    }

    @GetMapping("/unread-count")
    public long unreadCount() {
        return notificationService.countUnread(currentUser().getId());
    }

    @PostMapping("/{notificationId}/read")
    public void markRead(@PathVariable Long notificationId) {
        notificationService.markRead(notificationId, currentUser().getId());
    }

    @PostMapping("/read-all")
    public void markAllRead() {
        notificationService.markAllRead(currentUser().getId());
    }

    private User currentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userService.findByEmail(email);
    }
}
