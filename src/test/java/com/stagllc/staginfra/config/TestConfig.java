// src/test/java/com/stagllc/staginfra/config/TestConfig.java
package com.stagllc.staginfra.config;

import com.stagllc.staginfra.service.EmailService;
import com.stagllc.staginfra.service.RateLimiterService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl();
    }

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RateLimiterService rateLimiterService() {
        return new RateLimiterService();
    }

    @Bean
    @Primary
    public EmailService emailService() {
        // Create a no-op implementation for testing
        return new EmailService() {
            @Override
            public void sendVerificationEmail(String to, String token) {
                // Do nothing in tests
            }

            @Override
            public void sendWelcomeEmail(String to) {
                // Do nothing in tests
            }
        };
    }

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        // For tests, disable security completely
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}