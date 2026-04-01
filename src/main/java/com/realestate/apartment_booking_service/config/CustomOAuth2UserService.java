package com.realestate.apartment_booking_service.config;

import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.Role;
import com.realestate.apartment_booking_service.enums.UserStatus;
import com.realestate.apartment_booking_service.repositories.UserRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String email = oauth2User.getAttribute("email");
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"),
                    "Google account does not expose email");
        }

        User user = userRepository.findByEmail(email)
                .map(existing -> updateProfileIfNeeded(existing, oauth2User))
                .orElseGet(() -> createFromGoogle(email, oauth2User));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("access_denied"),
                    "User account is banned");
        }

        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
        attributes.put("email", email);

        return new DefaultOAuth2User(
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                attributes,
                "email");
    }

    private User createFromGoogle(String email, OAuth2User oauth2User) {
        String fullName = resolveDisplayName(email, oauth2User);
        String avatarUrl = oauth2User.getAttribute("picture");

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .fullName(fullName)
                .avatar(avatarUrl)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        return userRepository.save(user);
    }

    private User updateProfileIfNeeded(User user, OAuth2User oauth2User) {
        boolean changed = false;

        if (user.getFullName() == null || user.getFullName().isBlank()) {
            user.setFullName(resolveDisplayName(user.getEmail(), oauth2User));
            changed = true;
        }

        String avatarUrl = oauth2User.getAttribute("picture");
        if ((user.getAvatar() == null || user.getAvatar().isBlank())
                && avatarUrl != null
                && !avatarUrl.isBlank()) {
            user.setAvatar(avatarUrl);
            changed = true;
        }

        if (changed) {
            return userRepository.save(user);
        }

        return user;
    }

    private String resolveDisplayName(String email, OAuth2User oauth2User) {
        String name = oauth2User.getAttribute("name");
        if (name != null && !name.isBlank()) {
            return name;
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
