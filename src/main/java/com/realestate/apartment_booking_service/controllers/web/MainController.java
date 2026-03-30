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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final ApartmentService apartmentService;
    private final AgentProfileRepository agentProfileRepository;

    @GetMapping("/")
    public String home(@ModelAttribute ApartmentFilterRequest filterRequest, Model model) {
        if (filterRequest.getStatus() == null) {
            filterRequest.setStatus(ApartmentStatus.AVAILABLE);
        }
        model.addAttribute("apartments", apartmentService.search(filterRequest));
        model.addAttribute("filter", filterRequest);
        return "common/index";
    }

    @GetMapping("/search")
    public String search(@ModelAttribute ApartmentFilterRequest filterRequest, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("keyword", filterRequest.getKeyword());
        redirectAttributes.addAttribute("district", filterRequest.getDistrict());
        redirectAttributes.addAttribute("roomType", filterRequest.getRoomType());
        redirectAttributes.addAttribute(
                "transactionType",
                filterRequest.getTransactionType() == null ? null : filterRequest.getTransactionType().name());
        redirectAttributes.addAttribute(
                "status", filterRequest.getStatus() == null ? null : filterRequest.getStatus().name());
        return "redirect:/";
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
