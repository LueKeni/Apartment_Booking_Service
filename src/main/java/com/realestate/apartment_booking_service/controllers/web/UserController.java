package com.realestate.apartment_booking_service.controllers.web;

import com.realestate.apartment_booking_service.dto.BookingRequest;
import com.realestate.apartment_booking_service.dto.CreateReviewRequest;
import com.realestate.apartment_booking_service.entities.Apartment;
import com.realestate.apartment_booking_service.entities.Appointment;
import com.realestate.apartment_booking_service.entities.Favorite;
import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.Role;
import com.realestate.apartment_booking_service.enums.AppointmentStatus;
import com.realestate.apartment_booking_service.repositories.FavoriteRepository;
import com.realestate.apartment_booking_service.services.interfaces.ApartmentService;
import com.realestate.apartment_booking_service.services.interfaces.BookingService;
import com.realestate.apartment_booking_service.services.interfaces.NotificationService;
import com.realestate.apartment_booking_service.services.interfaces.ReviewService;
import com.realestate.apartment_booking_service.services.interfaces.UserService;
import com.realestate.apartment_booking_service.utils.SecurityUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final FavoriteRepository favoriteRepository;
    private final ApartmentService apartmentService;
    private final BookingService bookingService;
    private final NotificationService notificationService;
    private final ReviewService reviewService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User user = currentUser();
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<Appointment> appointments = bookingService.getUserAppointments(user.getId());

        long pendingAppointments = appointments.stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.PENDING)
                .count();
        long completedAppointments = appointments.stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.COMPLETED)
                .count();

        model.addAttribute("favoriteCount", favorites.size());
        model.addAttribute("appointmentCount", appointments.size());
        model.addAttribute("pendingAppointmentCount", pendingAppointments);
        model.addAttribute("completedAppointmentCount", completedAppointments);
        model.addAttribute("recentFavorites", favorites.stream().limit(4).toList());
        model.addAttribute("recentAppointments", appointments.stream().limit(4).toList());
        model.addAttribute("recentNotifications",
                notificationService.getNotifications(user.getId()).stream().limit(6).toList());
        model.addAttribute("unreadNotificationCount", notificationService.countUnread(user.getId()));
        return "user/dashboard";
    }

    @GetMapping("/favorites")
    public String favorites(Model model) {
        User user = currentUser();
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        model.addAttribute("favorites", favorites);
        model.addAttribute("favoriteCount", favorites.size());
        return "user/favorites";
    }

    @GetMapping("/appointments")
    public String appointments(Model model) {
        User user = currentUser();
        List<Appointment> appointments = bookingService.getUserAppointments(user.getId());

        long pendingAppointments = appointments.stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.PENDING)
                .count();
        long completedAppointments = appointments.stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.COMPLETED)
                .count();

        model.addAttribute("appointments", appointments);
        model.addAttribute("pendingAppointmentCount", pendingAppointments);
        model.addAttribute("completedAppointmentCount", completedAppointments);
        model.addAttribute("reviewedAppointmentIds", reviewService.getReviewedAppointmentIds(user.getId()));
        return "user/appointments";
    }

    @GetMapping("/notifications")
    public String notifications(Model model) {
        User user = currentUser();
        model.addAttribute("notifications", notificationService.getNotifications(user.getId()));
        model.addAttribute("unreadNotificationCount", notificationService.countUnread(user.getId()));
        return "user/notifications";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("userProfile", currentUser());
        return "user/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @RequestParam String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String avatar,
            @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile) {
        User user = currentUser();
        userService.updateProfile(user.getId(), fullName, phone, avatar, avatarFile);
        return "redirect:/user/profile?updated";
    }

    @PostMapping("/bookings")
    public String createBooking(@ModelAttribute BookingRequest request) {
        User user = currentUser();
        if (user.getRole() == Role.AGENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Agent cannot create booking");
        }
        bookingService.createBooking(user.getId(), request);
        return "redirect:/user/appointments";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id) {
        User user = currentUser();
        bookingService.cancelBooking(user.getId(), id);
        return "redirect:/user/appointments";
    }

    @PostMapping("/reviews")
    public String createReview(@ModelAttribute CreateReviewRequest request,
            @RequestParam(defaultValue = "false") boolean fromAppointments) {
        User user = currentUser();
        reviewService.createReview(user.getId(), request);
        if (fromAppointments) {
            return "redirect:/user/appointments?reviewed";
        }
        return "redirect:/user/appointments";
    }

    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(@PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean fromDashboard) {
        User user = currentUser();
        notificationService.markRead(id, user.getId());
        if (fromDashboard) {
            return "redirect:/user/dashboard?notificationRead";
        }
        return "redirect:/user/notifications?updated";
    }

    @PostMapping("/notifications/read-all")
    public String markAllNotificationsRead() {
        User user = currentUser();
        notificationService.markAllRead(user.getId());
        return "redirect:/user/notifications?updated";
    }

    @PostMapping("/favorites/{apartmentId}/toggle")
    public String toggleFavorite(@PathVariable Long apartmentId) {
        User user = currentUser();
        if (favoriteRepository.existsByUserIdAndApartmentId(user.getId(), apartmentId)) {
            favoriteRepository.deleteByUserIdAndApartmentId(user.getId(), apartmentId);
        } else {
            Apartment apartment = apartmentService.findById(apartmentId);
            Favorite favorite = Favorite.builder()
                    .user(user)
                    .apartment(apartment)
                    .build();
            favoriteRepository.save(favorite);
        }
        return "redirect:/apartments/" + apartmentId;
    }

    private User currentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userService.findByEmail(email);
    }
}
