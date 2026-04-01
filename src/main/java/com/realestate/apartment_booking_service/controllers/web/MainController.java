package com.realestate.apartment_booking_service.controllers.web;

import com.realestate.apartment_booking_service.dto.ApartmentFilterRequest;
import com.realestate.apartment_booking_service.entities.AgentProfile;
import com.realestate.apartment_booking_service.entities.Apartment;
import com.realestate.apartment_booking_service.enums.ApartmentStatus;
import com.realestate.apartment_booking_service.repositories.AgentProfileRepository;
import com.realestate.apartment_booking_service.repositories.ApartmentLikeRepository;
import com.realestate.apartment_booking_service.repositories.FavoriteRepository;
import com.realestate.apartment_booking_service.services.interfaces.ApartmentService;
import com.realestate.apartment_booking_service.services.interfaces.ReviewService;
import com.realestate.apartment_booking_service.services.interfaces.UserService;
import com.realestate.apartment_booking_service.utils.SecurityUtils;
import java.util.Set;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final ApartmentService apartmentService;
    private final AgentProfileRepository agentProfileRepository;
    private final ApartmentLikeRepository apartmentLikeRepository;
    private final FavoriteRepository favoriteRepository;
    private final ReviewService reviewService;
    private final UserService userService;

    @GetMapping("/")
    public String home(@ModelAttribute ApartmentFilterRequest filterRequest, Model model) {
        if (filterRequest.getStatus() == null) {
            filterRequest.setStatus(ApartmentStatus.AVAILABLE);
        }
        Long currentUserId = resolveCurrentUserId();
        
        // Fetch Top Agents for ranking (Top 5)
        var topAgents = agentProfileRepository.findTopAgents(org.springframework.data.domain.PageRequest.of(0, 5));

        model.addAttribute("apartments", apartmentService.search(filterRequest));
        model.addAttribute("filter", filterRequest);
        model.addAttribute("topAgents", topAgents);
        model.addAttribute("favoriteApartmentIds", resolveFavoriteApartmentIds(currentUserId));
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
        AgentProfile profile = 
                agentProfileRepository.findByUserId(apartment.getAgent().getId()).orElse(null);
        
        double averageRating = (profile != null) ? profile.getAverageRating() : 0.0;
        int reviewCount = (profile != null) ? profile.getReviewCount() : 0;
        
        Long currentUserId = resolveCurrentUserId();

        // Fetch Top Agents Ranking (Top 5)
        var topAgents = agentProfileRepository.findTopAgents(org.springframework.data.domain.PageRequest.of(0, 5));
        var bestAgent = topAgents.isEmpty() ? null : topAgents.get(0);

        model.addAttribute("apartment", apartment);
        model.addAttribute("relatedApartments", apartmentService.findRelatedApartments(id, apartment.getRoomType()));
        model.addAttribute("agentProfile", profile);
        model.addAttribute("agentReviewCount", reviewCount);
        model.addAttribute("agentAverageRating", String.format(Locale.US, "%.1f", averageRating));
        model.addAttribute("isLiked", currentUserId != null && apartmentService.isLiked(id, currentUserId));
        model.addAttribute("isFavorite", currentUserId != null && apartmentService.isFavorite(id, currentUserId));
        
        model.addAttribute("topAgents", topAgents);
        model.addAttribute("bestAgent", bestAgent);
        
        return "common/details";
    }

    @GetMapping("/agents/{agentId}/reviews")
    public String agentReviews(@PathVariable Long agentId, Model model) {
        AgentProfile profile = agentProfileRepository.findByUserId(agentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));

        var reviews = reviewService.getAgentReviews(agentId);
        var listings = apartmentService.findByAgent(agentId);

        model.addAttribute("agentProfile", profile);
        model.addAttribute("agentUser", profile.getUser());
        model.addAttribute("agentReviews", reviews);
        model.addAttribute("agentListings", listings);
        model.addAttribute("agentAverageRating", String.format(Locale.US, "%.1f", profile.getAverageRating()));
        model.addAttribute("agentReviewCount", profile.getReviewCount());
        return "common/agent-reviews";
    }

    private Long resolveCurrentUserId() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            return null;
        }
        return userService.findByEmail(email).getId();
    }

    private Set<Long> resolveFavoriteApartmentIds(Long currentUserId) {
        if (currentUserId == null) {
            return Set.of();
        }
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(currentUserId).stream()
                .map(favorite -> favorite.getApartment().getId())
                .collect(java.util.stream.Collectors.toSet());
    }

    private Set<Long> resolveLikedApartmentIds(Long currentUserId) {
        if (currentUserId == null) {
            return Set.of();
        }
        return apartmentLikeRepository.findByUserId(currentUserId).stream()
                .map(apartmentLike -> apartmentLike.getApartment().getId())
                .collect(java.util.stream.Collectors.toSet());
    }
}
