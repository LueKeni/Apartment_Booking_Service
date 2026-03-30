package com.realestate.apartment_booking_service.services.impl;

import com.realestate.apartment_booking_service.dto.RegisterRequest;
import com.realestate.apartment_booking_service.entities.AgentProfile;
import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.Role;
import com.realestate.apartment_booking_service.enums.UserStatus;
import com.realestate.apartment_booking_service.repositories.AgentProfileRepository;
import com.realestate.apartment_booking_service.repositories.UserRepository;
import com.realestate.apartment_booking_service.services.interfaces.UserService;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AgentProfileRepository agentProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        return userRepository.save(user);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Override
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User updateUserStatus(Long userId, boolean banned) {
        User user = findById(userId);
        user.setStatus(banned ? UserStatus.BANNED : UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    @Override
    public User verifyAgent(Long userId, boolean verified) {
        User user = findById(userId);
        if (user.getRole() != Role.AGENT) {
            user.setRole(Role.AGENT);
            userRepository.save(user);
        }

        AgentProfile profile = agentProfileRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultAgentProfile(user));

        profile.setVerifiedStatus(verified);
        agentProfileRepository.save(profile);
        return user;
    }

    @Override
    public User updateUserRole(Long userId, Role role) {
        User user = findById(userId);
        user.setRole(role);
        User savedUser = userRepository.save(user);

        if (role == Role.AGENT) {
            agentProfileRepository.findByUserId(userId)
                    .orElseGet(() -> agentProfileRepository.save(createDefaultAgentProfile(savedUser)));
        }

        return savedUser;
    }

    @Override
    public User updateProfile(Long userId, String fullName, String phone, String avatar) {
        User user = findById(userId);

        if (fullName == null || fullName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required");
        }

        user.setFullName(fullName.trim());
        user.setPhone(normalizeBlankToNull(phone));
        user.setAvatar(normalizeBlankToNull(avatar));
        return userRepository.save(user);
    }

    private AgentProfile createDefaultAgentProfile(User user) {
        return AgentProfile.builder()
                .user(user)
                .responseRate(0.0)
                .activeListings(0)
                .successDeals(0)
                .verifiedStatus(false)
                .build();
    }

    private String normalizeBlankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
