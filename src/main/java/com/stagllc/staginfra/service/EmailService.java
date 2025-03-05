// src/main/java/com/stagllc/staginfra/service/EmailService.java
package com.stagllc.staginfra.service;

// Make the EmailService an interface to allow for easier mocking
public interface EmailService {
    void sendVerificationEmail(String to, String token);
    void sendWelcomeEmail(String to);
}