package com.realestate.apartment_booking_service.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.util.StringUtils;

@Configuration
public class GoogleOAuthClientConfig {

    @Bean
    @ConditionalOnMissingBean(ClientRegistrationRepository.class)
    @Conditional(GoogleOAuthCredentialsPresentCondition.class)
    public ClientRegistrationRepository googleClientRegistrationRepository(
            Environment environment) {

        String clientId = firstNonBlank(
                environment.getProperty("google.oauth.client-id"),
                environment.getProperty("spring.security.oauth2.client.registration.google.client-id"),
                environment.getProperty("GOOGLE_CLIENT_ID"),
                environment.getProperty("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID"));

        String clientSecret = firstNonBlank(
                environment.getProperty("google.oauth.client-secret"),
                environment.getProperty("spring.security.oauth2.client.registration.google.client-secret"),
                environment.getProperty("GOOGLE_CLIENT_SECRET"),
                environment.getProperty("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET"));

        ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();

        return new InMemoryClientRegistrationRepository(google);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    static class GoogleOAuthCredentialsPresentCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            Environment environment = context.getEnvironment();

            String clientId = firstNonBlank(
                    environment.getProperty("google.oauth.client-id"),
                    environment.getProperty("spring.security.oauth2.client.registration.google.client-id"),
                    environment.getProperty("GOOGLE_CLIENT_ID"),
                    environment.getProperty("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID"));

            String clientSecret = firstNonBlank(
                    environment.getProperty("google.oauth.client-secret"),
                    environment.getProperty("spring.security.oauth2.client.registration.google.client-secret"),
                    environment.getProperty("GOOGLE_CLIENT_SECRET"),
                    environment.getProperty("SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET"));

            return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
        }
    }
}