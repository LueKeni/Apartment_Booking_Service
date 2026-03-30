package com.realestate.apartment_booking_service.controllers.web;

import com.realestate.apartment_booking_service.entities.Appointment;
import com.realestate.apartment_booking_service.entities.Conversation;
import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.ApartmentStatus;
import com.realestate.apartment_booking_service.services.interfaces.ApartmentService;
import com.realestate.apartment_booking_service.services.interfaces.UserService;
import com.realestate.apartment_booking_service.repositories.ApartmentRepository;
import com.realestate.apartment_booking_service.repositories.AppointmentRepository;
import com.realestate.apartment_booking_service.repositories.ConversationRepository;
import com.realestate.apartment_booking_service.repositories.UserRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final ApartmentService apartmentService;
    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final AppointmentRepository appointmentRepository;
    private final ConversationRepository conversationRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<User> latestUsers = userRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .limit(6)
                .toList();

        List<Appointment> latestAppointments = appointmentRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Appointment::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .limit(6)
                .toList();

        List<Conversation> latestConversations = conversationRepository.findAll()
                .stream()
                .sorted(Comparator
                        .comparing(Conversation::getLastMessageAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .limit(6)
                .toList();

        long hiddenListings = apartmentRepository.findAll()
                .stream()
                .filter(apartment -> apartment.getStatus() == ApartmentStatus.HIDDEN)
                .count();

        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("listingCount", apartmentRepository.count());
        model.addAttribute("appointmentCount", appointmentRepository.count());
        model.addAttribute("conversationCount", conversationRepository.count());
        model.addAttribute("hiddenListingCount", hiddenListings);
        model.addAttribute("latestUsers", latestUsers);
        model.addAttribute("latestAppointments", latestAppointments);
        model.addAttribute("latestConversations", latestConversations);
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users";
    }

    @PostMapping("/users/{id}/ban")
    public String banUser(@PathVariable Long id, @RequestParam boolean banned) {
        userService.updateUserStatus(id, banned);
        return "redirect:/admin/users?updated";
    }

    @PostMapping("/users/{id}/verify-agent")
    public String verifyAgent(@PathVariable Long id, @RequestParam boolean verified) {
        userService.verifyAgent(id, verified);
        return "redirect:/admin/users?verified";
    }

    @GetMapping("/listings")
    public String listings(Model model) {
        model.addAttribute("apartments", apartmentRepository.findAll());
        return "admin/listings";
    }

    @PostMapping("/listings/{id}/status")
    public String updateListingStatus(@PathVariable Long id, @RequestParam String status) {
        apartmentService.updateStatus(id, status);
        return "redirect:/admin/listings?updated";
    }

    @GetMapping("/appointments")
    public String appointments(Model model) {
        model.addAttribute("appointments", appointmentRepository.findAll());
        return "admin/appointments";
    }

    @GetMapping("/conversations")
    public String conversations(Model model) {
        model.addAttribute("conversations", conversationRepository.findAll());
        return "admin/conversations";
    }
}
