package com.realestate.apartment_booking_service.services.interfaces;

import com.realestate.apartment_booking_service.dto.RegisterRequest;
import com.realestate.apartment_booking_service.entities.AgentProfile;
import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.Role;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    User registerUser(RegisterRequest request);

    User findByEmail(String email);

    User findById(Long userId);

    List<User> findAll();

    User updateUserStatus(Long userId, boolean banned);

    User verifyAgent(Long userId, boolean verified);

    AgentProfile findAgentProfileByUserId(Long userId);

    AgentProfile submitAgentVerification(Long userId, MultipartFile idCardFile, MultipartFile portraitFile);

    boolean canAgentPublishListing(Long userId);

    User updateUserRole(Long userId, Role role);

    User updateProfile(Long userId, String fullName, String phone, String avatar, MultipartFile avatarFile);
}
