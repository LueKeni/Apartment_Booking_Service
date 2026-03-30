package com.realestate.apartment_booking_service.config;

import com.realestate.apartment_booking_service.entities.AgentProfile;
import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.Role;
import com.realestate.apartment_booking_service.enums.UserStatus;
import com.realestate.apartment_booking_service.repositories.AgentProfileRepository;
import com.realestate.apartment_booking_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@realestate.local")) {
            User admin = User.builder()
                    .email("admin@realestate.local")
                    .password(passwordEncoder.encode("Admin@123"))
                    .fullName("System Admin")
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(admin);
        }

        if (!userRepository.existsByEmail("agent@realestate.local")) {
            User agent = User.builder()
                    .email("agent@realestate.local")
                    .password(passwordEncoder.encode("Agent@123"))
                    .fullName("Default Agent")
                    .phone("0900000001")
                    .role(Role.AGENT)
                    .status(UserStatus.ACTIVE)
                    .build();
            User savedAgent = userRepository.save(agent);

            AgentProfile profile = AgentProfile.builder()
                    .user(savedAgent)
                    .location("Ho Chi Minh City")
                    .bio("Apartment consultant specializing in rental and resale apartments.")
                    .responseRate(94.0)
                    .activeListings(0)
                    .successDeals(0)
                    .verifiedStatus(true)
                    .build();
            agentProfileRepository.save(profile);
        }

        if (!userRepository.existsByEmail("user@realestate.local")) {
            User user = User.builder()
                    .email("user@realestate.local")
                    .password(passwordEncoder.encode("User@123"))
                    .fullName("Default User")
                    .phone("0900000002")
                    .role(Role.USER)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);
        }
    }
}
