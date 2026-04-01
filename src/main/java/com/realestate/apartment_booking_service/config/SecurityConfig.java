package com.realestate.apartment_booking_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        @Autowired
        private CustomOAuth2SuccessHandler customOAuth2SuccessHandler;

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
                                                .successHandler(customOAuth2SuccessHandler))
                                // --------------------------------------
                                .logout(logout -> logout
                                                .logoutUrl("/auth/logout")
                                                .logoutSuccessUrl("/auth/login?logout")
                                                .permitAll())
                                .httpBasic(Customizer.withDefaults());

                return http.build();
        }

}