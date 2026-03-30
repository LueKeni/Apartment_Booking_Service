package com.realestate.apartment_booking_service.controllers.web;

import com.realestate.apartment_booking_service.entities.Apartment;
import com.realestate.apartment_booking_service.entities.Appointment;
import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.ApartmentStatus;
import com.realestate.apartment_booking_service.enums.AppointmentStatus;
import com.realestate.apartment_booking_service.services.interfaces.ApartmentService;
import com.realestate.apartment_booking_service.services.interfaces.BookingService;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
@RequestMapping("/agent")
public class AgentController {

    private final ApartmentService apartmentService;
    private final BookingService bookingService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User agent = currentUser();
        List<Appointment> appointments = bookingService.getAgentAppointments(agent.getId());
        List<Apartment> apartments = apartmentService.findByAgent(agent.getId());

        long completed = appointments
                .stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .count();
        long pending = appointments
                .stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING)
                .count();
        long hiddenListings = apartments
                .stream()
                .filter(apartment -> apartment.getStatus() == ApartmentStatus.HIDDEN)
                .count();

        model.addAttribute("appointmentCount", appointments.size());
        model.addAttribute("completedCount", completed);
        model.addAttribute("pendingCount", pending);
        model.addAttribute("listingCount", apartments.size());
        model.addAttribute("hiddenListingCount", hiddenListings);
        model.addAttribute("recentAppointments", appointments.stream().limit(6).toList());
        model.addAttribute("recentListings", apartments.stream().limit(6).toList());
        return "agent/dashboard";
    }

    @GetMapping("/listings")
    public String listings(@RequestParam(required = false) Long editId, Model model) {
        User agent = currentUser();
        model.addAttribute("apartments", apartmentService.findByAgent(agent.getId()));
        model.addAttribute("apartment", new Apartment());

        if (editId != null) {
            Apartment editApartment = apartmentService.findById(editId);
            if (!editApartment.getAgent().getId().equals(agent.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No access to this apartment");
            }
            model.addAttribute("editApartment", editApartment);
        }
        return "agent/listings";
    }

    @PostMapping("/listings")
    public String saveListing(
            @ModelAttribute Apartment apartment,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles) {
        User agent = currentUser();
        apartmentService.createApartment(apartment, agent.getId(), imageFiles);
        return "redirect:/agent/listings?saved";
    }

    @PostMapping("/listings/{id}")
    public String updateListing(
            @PathVariable Long id,
            @ModelAttribute Apartment apartment,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles) {
        User agent = currentUser();
        apartmentService.updateApartment(id, apartment, agent.getId(), imageFiles);
        return "redirect:/agent/listings?updated";
    }

    @PostMapping("/listings/{id}/visibility")
    public String updateListingVisibility(@PathVariable Long id, @RequestParam boolean hidden) {
        User agent = currentUser();
        apartmentService.updateStatusForAgent(id, hidden ? ApartmentStatus.HIDDEN : ApartmentStatus.AVAILABLE, agent.getId());
        return "redirect:/agent/listings?statusUpdated";
    }

    @PostMapping("/listings/{id}/delete")
    public String deleteListing(@PathVariable Long id) {
        User agent = currentUser();
        apartmentService.deleteApartment(id, agent.getId());
        return "redirect:/agent/listings?deleted";
    }

    @GetMapping("/bookings")
    public String bookings(Model model) {
        User agent = currentUser();
        model.addAttribute("appointments", bookingService.getAgentAppointments(agent.getId()));
        return "agent/bookings";
    }

    @PostMapping("/bookings/{id}/status")
    public String updateBookingStatus(
            @PathVariable Long id,
            @RequestParam AppointmentStatus status,
            @RequestParam(required = false) String agentNote) {
        User agent = currentUser();
        bookingService.updateBookingStatus(agent.getId(), id, status, agentNote);
        return "redirect:/agent/bookings?statusUpdated";
    }

    private User currentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userService.findByEmail(email);
    }
}
