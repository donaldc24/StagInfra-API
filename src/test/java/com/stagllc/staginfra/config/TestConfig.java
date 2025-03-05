// src/test/java/com/stagllc/staginfra/config/TestConfig.java
package com.stagllc.staginfra.config;

import com.stagllc.staginfra.service.EmailService;
import com.stagllc.staginfra.service.RateLimiterService;
import com.stagllc.staginfra.service.UserService;
import com.stagllc.staginfra.service.UserServiceImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RateLimiterService rateLimiterService() {
        return new RateLimiterService();
    }

    @Bean
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
}