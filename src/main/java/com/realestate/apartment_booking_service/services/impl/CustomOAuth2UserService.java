package com.realestate.apartment_booking_service.services.impl;

import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.Role;
import com.realestate.apartment_booking_service.enums.UserStatus;
import com.realestate.apartment_booking_service.repositories.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();

        String email = extractEmail(registrationId, attributes);
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_request"), "Cannot get email from " + registrationId + " account");
        }

        String fullName = extractFullName(attributes, email);
        String avatar = extractAvatar(registrationId, attributes);

        User user = userRepository.findByEmail(email)
                .map(existing -> updateProfileIfMissing(existing, fullName, avatar))
                .orElseGet(() -> createOAuthUser(email, fullName, avatar));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new OAuth2AuthenticationException(new OAuth2Error("access_denied"), "User is banned");
        }

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        Map<String, Object> mappedAttributes = new HashMap<>(attributes);
        mappedAttributes.put("email", user.getEmail());
        mappedAttributes.put("name", user.getFullName());
        if (user.getAvatarUrl() != null) {
            mappedAttributes.put("picture", user.getAvatarUrl());
        }

        return new DefaultOAuth2User(authorities, mappedAttributes, "email");
    }

    private User createOAuthUser(String email, String fullName, String avatar) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .fullName(fullName)
                .avatar(avatar)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }

    private User updateProfileIfMissing(User user, String fullName, String avatar) {
        boolean dirty = false;
        if ((user.getFullName() == null || user.getFullName().isBlank()) && fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
            dirty = true;
        }
        if ((user.getAvatar() == null || user.getAvatar().isBlank()) && avatar != null && !avatar.isBlank()) {
            user.setAvatar(avatar);
            dirty = true;
        }
        if (dirty) {
            return userRepository.save(user);
        }
        return user;
    }

    private String extractEmail(String registrationId, Map<String, Object> attributes) {
        Object email = attributes.get("email");
        if (email != null) {
            return email.toString();
        }
        if ("facebook".equalsIgnoreCase(registrationId)) {
            Object id = attributes.get("id");
            if (id != null) {
                return id + "@facebook.local";
            }
        }
        return null;
    }

    private String extractFullName(Map<String, Object> attributes, String email) {
        Object name = attributes.get("name");
        if (name != null && !name.toString().isBlank()) {
            return name.toString();
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    @SuppressWarnings("unchecked")
    private String extractAvatar(String registrationId, Map<String, Object> attributes) {
        Object picture = attributes.get("picture");
        if (picture == null) {
            return null;
        }

        if ("google".equalsIgnoreCase(registrationId) && picture instanceof String url) {
            return url;
        }

        if ("facebook".equalsIgnoreCase(registrationId) && picture instanceof Map<?, ?> pictureMap) {
            Object data = pictureMap.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                Object url = dataMap.get("url");
                return url == null ? null : url.toString();
            }
        }
        return null;
    }
}
