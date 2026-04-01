package com.realestate.apartment_booking_service.config;

import com.realestate.apartment_booking_service.entities.User;
import com.realestate.apartment_booking_service.enums.Role;
import com.realestate.apartment_booking_service.repositories.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        // Lấy thông tin người dùng từ Google/Facebook trả về
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        
        // Google trả về "picture", Facebook thường trả về qua "picture.data.url" (Spring Security có thể map sẵn)
        String avatar = oauthUser.getAttribute("picture"); 

        if (email != null) {
            // Kiểm tra xem user đã tồn tại trong DB chưa
            Optional<User> userOptional = userRepository.findByEmail(email);

            if (userOptional.isEmpty()) {
                // Nếu chưa có, tiến hành tạo mới tài khoản
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setFullName(name);
                newUser.setAvatar(avatar);
                
                // Set mật khẩu ngẫu nhiên cho tài khoản social (vì họ đăng nhập bằng OAuth2)
                newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                
                // Mặc định tài khoản tạo mới sẽ có quyền USER
                newUser.setRole(Role.USER);
                // Giả sử entity User của bạn có status ACTIVE mặc định, hoặc bạn set bằng Enum
                // newUser.setStatus(UserStatus.ACTIVE); 

                userRepository.save(newUser);
            }
        }

        // Chuyển hướng người dùng về trang chủ sau khi xử lý xong
        response.sendRedirect("/");
    }
}