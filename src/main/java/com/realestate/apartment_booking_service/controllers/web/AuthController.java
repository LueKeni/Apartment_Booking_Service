package com.realestate.apartment_booking_service.controllers.web;

import com.realestate.apartment_booking_service.dto.RegisterRequest;
import com.realestate.apartment_booking_service.services.interfaces.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
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
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;
    private final Environment environment;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/google-login")
    public String googleLogin() {
        if (isGoogleLoginEnabled()) {
            return "redirect:/oauth2/authorization/google";
        }
        return "redirect:/auth/login?googleNotConfigured";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerUser(request);
            return "redirect:/auth/login?registered";
        } catch (ResponseStatusException ex) {
            model.addAttribute("error", ex.getReason());
            return "auth/register";
        }
    }

    private boolean isGoogleLoginEnabled() {
        ClientRegistrationRepository repository = clientRegistrationRepositoryProvider.getIfAvailable();
        if (repository instanceof InMemoryClientRegistrationRepository inMemoryRepository) {
            return inMemoryRepository.findByRegistrationId("google") != null;
        }

        if (repository != null) {
            return true;
        }

        String clientId = firstNonBlank(
                environment.getProperty("google.oauth.client-id"),
                environment.getProperty("spring.security.oauth2.client.registration.google.client-id"),
                environment.getProperty("GOOGLE_CLIENT_ID"),
                environment.getProperty("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID"));

        String clientSecret = firstNonBlank(
                environment.getProperty("google.oauth.client-secret"),
                environment.getProperty("spring.security.oauth2.client.registration.google.client-secret"),
                environment.getProperty("GOOGLE_CLIENT_SECRET"),
                environment.getProperty("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET"));

        return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
