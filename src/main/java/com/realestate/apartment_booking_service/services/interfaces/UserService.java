package com.realestate.apartment_booking_service.services.interfaces;

import com.realestate.apartment_booking_service.dto.RegisterRequest;
import com.realestate.apartment_booking_service.entities.User;
import java.util.List;

public interface UserService {

    User registerUser(RegisterRequest request);

    User findByEmail(String email);

    User findById(Long userId);

    List<User> findAll();

    User updateUserStatus(Long userId, boolean banned);

    User verifyAgent(Long userId, boolean verified);
}
