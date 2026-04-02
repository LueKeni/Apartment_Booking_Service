package com.realestate.apartment_booking_service.config;

import com.realestate.apartment_booking_service.services.impl.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        private final CustomOAuth2UserService customOAuth2UserService;

        public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
                this.customOAuth2UserService = customOAuth2UserService;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/",
                                                                "/search",
                                                                "/apartments/**",
                                                                "/auth/**",
                                                                "/oauth2/**",
                                                                "/login/oauth2/**",
                                                                "/error",
                                                                "/api/assistant/**",
                                                                "/momo/**",
                                                                "/css/**",
                                                                "/js/**",
                                                                "/images/**",
                                                                "/uploads/**")
                                                .permitAll()
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/agent/**").hasRole("AGENT")
                                                .requestMatchers("/user/**").hasAnyRole("USER", "AGENT", "ADMIN")
                                                .requestMatchers("/api/**").authenticated()
                                                .requestMatchers("/uploads/**").permitAll()
                                                .anyRequest().authenticated())

                                .formLogin(form -> form
                                                .loginPage("/auth/login")
                                                .loginProcessingUrl("/auth/login")
                                                .defaultSuccessUrl("/", true)
                                                .permitAll())
                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/auth/login")
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .defaultSuccessUrl("/", true)
                                                .failureUrl("/auth/login?error"))
                                // --------------------------------------
                                .logout(logout -> logout
                                                .logoutUrl("/auth/logout")
                                                .logoutSuccessUrl("/auth/login?logout")
                                                .permitAll())
                                .httpBasic(Customizer.withDefaults());

                return http.build();
        }

}
