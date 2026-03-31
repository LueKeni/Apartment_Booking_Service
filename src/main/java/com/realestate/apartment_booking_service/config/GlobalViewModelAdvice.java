package com.realestate.apartment_booking_service.config;

import com.realestate.apartment_booking_service.services.interfaces.UserService;
import com.realestate.apartment_booking_service.utils.SecurityUtils;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class GlobalViewModelAdvice {

    private final UserService userService;

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    @ModelAttribute("currentEmail")
    public String currentEmail() {
        if (!isAuthenticated()) {
            return null;
        }
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @ModelAttribute("currentUserId")
    public Long currentUserId() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null) {
            return null;
        }
        return userService.findByEmail(email).getId();
    }

    @ModelAttribute("hasRoleUser")
    public boolean hasRoleUser() {
        return hasAnyRole("ROLE_USER", "ROLE_AGENT", "ROLE_ADMIN");
    }

    @ModelAttribute("hasRoleAgent")
    public boolean hasRoleAgent() {
        return hasAnyRole("ROLE_AGENT", "ROLE_ADMIN");
    }

    @ModelAttribute("hasRoleAdmin")
    public boolean hasRoleAdmin() {
        return hasAnyRole("ROLE_ADMIN");
    }

    private boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Set<String> userRoles = authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .collect(java.util.stream.Collectors.toSet());
        for (String role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
