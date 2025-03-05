// src/main/java/com/stagllc/staginfra/service/EmailServiceImpl.java
package com.stagllc.staginfra.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String baseUrl;
    private final String fromEmail;

    public EmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl,
            @Value("${spring.mail.username:noreply@example.com}") String fromEmail) {
        this.mailSender = mailSender;
        this.baseUrl = baseUrl;
        this.fromEmail = fromEmail;
    }

    @Async
    @Override
    public void sendVerificationEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Verify your Cloud Architecture Designer account");

        String verificationUrl = baseUrl + "/verify-email?token=" + token;

        message.setText("Hello,\n\n" +
                "Thank you for registering with Cloud Architecture Designer! " +
                "Please click the link below to verify your email address:\n\n" +
                verificationUrl + "\n\n" +
                "This link will expire in 48 hours.\n\n" +
                "Best regards,\n" +
                "Cloud Architecture Designer Team");

        mailSender.send(message);
    }

    @Async
    @Override
    public void sendWelcomeEmail(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Welcome to Cloud Architecture Designer!");

        message.setText("Hello,\n\n" +
                "Thank you for verifying your email address. Your account is now active!\n\n" +
                "You can now start designing your cloud architecture at " + baseUrl + "\n\n" +
                "Best regards,\n" +
                "Cloud Architecture Designer Team");

        mailSender.send(message);
    }
}