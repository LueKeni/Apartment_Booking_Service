package com.realestate.apartment_booking_service.controllers.web;

import com.realestate.apartment_booking_service.dto.RegisterRequest;
import com.realestate.apartment_booking_service.services.interfaces.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.facebook.client-id:}")
    private String facebookClientId;

    @Value("${spring.security.oauth2.client.registration.facebook.client-secret:}")
    private String facebookClientSecret;

    @GetMapping("/login")
    public String loginPage(Model model) {
        applySocialLoginFlags(model);
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        applySocialLoginFlags(model);
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            applySocialLoginFlags(model);
            return "auth/register";
        }

        try {
            userService.registerUser(request);
            return "redirect:/auth/login?registered";
        } catch (ResponseStatusException ex) {
            model.addAttribute("error", ex.getReason());
            applySocialLoginFlags(model);
            return "auth/register";
        }
    }

    private void applySocialLoginFlags(Model model) {
        boolean googleOAuthEnabled = isConfiguredClient(googleClientId, googleClientSecret);
        boolean facebookOAuthEnabled = isConfiguredClient(facebookClientId, facebookClientSecret);

        model.addAttribute("googleOAuthEnabled", googleOAuthEnabled);
        model.addAttribute("facebookOAuthEnabled", facebookOAuthEnabled);
        model.addAttribute("socialOAuthEnabled", googleOAuthEnabled || facebookOAuthEnabled);
    }

    private boolean isConfiguredClient(String clientId, String clientSecret) {
        return isConfiguredValue(clientId) && isConfiguredValue(clientSecret);
    }

    private boolean isConfiguredValue(String value) {
        return value != null
                && !value.isBlank()
                && !value.contains("${")
                && !value.startsWith("missing-")
                && !value.startsWith("your-")
                && !value.startsWith("paste-");
    }
}
