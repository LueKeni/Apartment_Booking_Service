package com.realestate.apartment_booking_service.services.interfaces;

import com.realestate.apartment_booking_service.dto.RegisterRequest;
import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.Role;
import java.util.List;

public interface UserService {

    User registerUser(RegisterRequest request);

    User findByEmail(String email);

    User findById(Long userId);

    List<User> findAll();

    User updateUserStatus(Long userId, boolean banned);

    User verifyAgent(Long userId, boolean verified);

    User updateUserRole(Long userId, Role role);

    User updateProfile(Long userId, String fullName, String phone, String avatar);
}
