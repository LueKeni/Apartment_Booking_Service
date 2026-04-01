package com.realestate.apartment_booking_service.controllers.web;

import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.ApartmentStatus;
import com.realestate.apartment_booking_service.enums.PaymentStatus;
import com.realestate.apartment_booking_service.enums.Role;
import com.realestate.apartment_booking_service.services.interfaces.ApartmentService;
import com.realestate.apartment_booking_service.services.interfaces.UserService;
import com.realestate.apartment_booking_service.utils.SecurityUtils;
import com.realestate.apartment_booking_service.repositories.ApartmentRepository;
import com.realestate.apartment_booking_service.repositories.PointTopUpRepository;
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
    private final PointTopUpRepository pointTopUpRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<User> latestUsers = userRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .limit(6)
                .toList();

        long hiddenListings = apartmentRepository.findAll()
                .stream()
            .filter(apartment -> apartment.getStatus() == ApartmentStatus.COMING_SOON
                || apartment.getStatus() == ApartmentStatus.HIDDEN)
                .count();

        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("listingCount", apartmentRepository.count());
        model.addAttribute("hiddenListingCount", hiddenListings);
        model.addAttribute("pointRevenue", pointTopUpRepository.sumAmountByStatus(PaymentStatus.SUCCESS));
        var monthlyRevenue = pointTopUpRepository.findMonthlyRevenue();
        model.addAttribute("revenueLabels", monthlyRevenue.stream()
                .map(PointTopUpRepository.MonthlyRevenueProjection::getMonth)
                .toList());
        model.addAttribute("revenueValues", monthlyRevenue.stream()
                .map(row -> row.getTotal() == null ? 0 : row.getTotal().doubleValue())
                .toList());
        model.addAttribute("latestUsers", latestUsers);
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

    @PostMapping("/users/{id}/role")
    public String updateUserRole(@PathVariable Long id, @RequestParam Role role) {
        User targetUser = userService.findById(id);
        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        if (currentUserEmail != null && currentUserEmail.equalsIgnoreCase(targetUser.getEmail())) {
            return "redirect:/admin/users?roleDenied";
        }

        userService.updateUserRole(id, role);
        return "redirect:/admin/users?roleUpdated";
    }

    @GetMapping("/listings")
    public String listings(Model model) {
        model.addAttribute("apartments", apartmentRepository.findAll());
        return "admin/listings";
    }

    @GetMapping("/points")
    public String pointHistory(Model model) {
        model.addAttribute("pointTopups",
                pointTopUpRepository.findTop200ByUserRoleOrderByCreatedAtDesc(Role.AGENT));
        return "admin/points";
    }

    @PostMapping("/listings/{id}/status")
    public String updateListingStatus(@PathVariable Long id, @RequestParam String status) {
        apartmentService.updateStatus(id, status);
        return "redirect:/admin/listings?updated";
    }

}
