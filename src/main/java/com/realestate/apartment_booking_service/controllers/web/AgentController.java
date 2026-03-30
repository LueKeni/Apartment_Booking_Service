package com.realestate.apartment_booking_service.controllers.web;

import com.realestate.apartment_booking_service.entities.AgentSchedule;
import com.realestate.apartment_booking_service.entities.Apartment;
import com.realestate.apartment_booking_service.entities.Appointment;
import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.ApartmentStatus;
import com.realestate.apartment_booking_service.enums.AppointmentStatus;
import com.realestate.apartment_booking_service.repositories.AgentScheduleRepository;
import com.realestate.apartment_booking_service.services.interfaces.ApartmentService;
import com.realestate.apartment_booking_service.services.interfaces.BookingService;
import com.realestate.apartment_booking_service.services.interfaces.UserService;
import com.realestate.apartment_booking_service.utils.SecurityUtils;
import java.time.LocalDate;
import java.util.Arrays;
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

@Controller
@RequiredArgsConstructor
@RequestMapping("/agent")
public class AgentController {

    private final ApartmentService apartmentService;
    private final BookingService bookingService;
    private final AgentScheduleRepository agentScheduleRepository;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User agent = currentUser();
        List<Appointment> appointments = bookingService.getAgentAppointments(agent.getId());
        List<Apartment> apartments = apartmentService.findByAgent(agent.getId());
        List<AgentSchedule> upcomingSchedules = agentScheduleRepository
                .findByAgentIdAndAvailableDateGreaterThanEqualOrderByAvailableDateAsc(agent.getId(), LocalDate.now())
                .stream()
                .limit(5)
                .toList();

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
        model.addAttribute("upcomingSchedules", upcomingSchedules);
        return "agent/dashboard";
    }

    @GetMapping("/listings")
    public String listings(Model model) {
        User agent = currentUser();
        model.addAttribute("apartments", apartmentService.findByAgent(agent.getId()));
        model.addAttribute("apartment", new Apartment());
        return "agent/listings";
    }

    @PostMapping("/listings")
    public String saveListing(@ModelAttribute Apartment apartment) {
        User agent = currentUser();
        apartmentService.createApartment(apartment, agent.getId());
        return "redirect:/agent/listings?saved";
    }

    @PostMapping("/listings/{id}")
    public String updateListing(@PathVariable Long id, @ModelAttribute Apartment apartment) {
        User agent = currentUser();
        apartmentService.updateApartment(id, apartment, agent.getId());
        return "redirect:/agent/listings?updated";
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

    @GetMapping("/schedule")
    public String schedule(Model model) {
        User agent = currentUser();
        model.addAttribute(
                "schedules",
                agentScheduleRepository.findByAgentIdAndAvailableDateGreaterThanEqualOrderByAvailableDateAsc(
                        agent.getId(), LocalDate.now()));
        return "agent/schedule";
    }

    @PostMapping("/schedule")
    public String saveSchedule(@RequestParam LocalDate availableDate, @RequestParam List<String> timeSlots) {
        User agent = currentUser();
        List<String> normalizedSlots = normalizeSlots(timeSlots);

        AgentSchedule schedule = agentScheduleRepository.findByAgentIdAndAvailableDate(agent.getId(), availableDate)
                .orElseGet(() -> AgentSchedule.builder().agent(agent).availableDate(availableDate).build());

        schedule.setTimeSlots(normalizedSlots);
        agentScheduleRepository.save(schedule);

        return "redirect:/agent/schedule?saved";
    }

    private List<String> normalizeSlots(List<String> rawSlots) {
        return rawSlots.stream()
                .flatMap(raw -> Arrays.stream(raw.split(",")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private User currentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userService.findByEmail(email);
    }
}
