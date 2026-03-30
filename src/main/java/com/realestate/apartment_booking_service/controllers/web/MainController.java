package com.realestate.apartment_booking_service.controllers.web;

import com.realestate.apartment_booking_service.dto.ApartmentFilterRequest;
import com.realestate.apartment_booking_service.entities.Apartment;
import com.realestate.apartment_booking_service.enums.ApartmentStatus;
import com.realestate.apartment_booking_service.repositories.AgentProfileRepository;
import com.realestate.apartment_booking_service.services.interfaces.ApartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final ApartmentService apartmentService;
    private final AgentProfileRepository agentProfileRepository;

    @GetMapping("/")
    public String home(Model model) {
        ApartmentFilterRequest filter = new ApartmentFilterRequest();
        filter.setStatus(ApartmentStatus.AVAILABLE);
        model.addAttribute("apartments", apartmentService.search(filter));
        return "common/index";
    }

    @GetMapping("/search")
    public String search(@ModelAttribute ApartmentFilterRequest filterRequest, Model model) {
        model.addAttribute("apartments", apartmentService.search(filterRequest));
        model.addAttribute("filter", filterRequest);
        return "common/search";
    }

    @GetMapping("/apartments/{id}")
    public String apartmentDetails(@PathVariable Long id, Model model) {
        Apartment apartment = apartmentService.findById(id);
        model.addAttribute("apartment", apartment);
        model.addAttribute("agentProfile",
                agentProfileRepository.findByUserId(apartment.getAgent().getId()).orElse(null));
        return "common/details";
    }
}
