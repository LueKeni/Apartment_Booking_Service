package com.realestate.apartment_booking_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider,
                        CustomOAuth2UserService customOAuth2UserService)
                        throws Exception {
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
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/auth/login")
                                                .loginProcessingUrl("/auth/login")
                                                .defaultSuccessUrl("/", true)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/auth/logout")
                                                .logoutSuccessUrl("/auth/login?logout")
                                                .permitAll())
                                .httpBasic(Customizer.withDefaults());

                if (clientRegistrationRepositoryProvider.getIfAvailable() != null) {
                        http.oauth2Login(oauth2 -> oauth2
                                        .loginPage("/auth/login")
                                        .defaultSuccessUrl("/", true)
                                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)));
                }

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
