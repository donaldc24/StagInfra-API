// src/main/java/com/stagllc/staginfra/service/UserService.java
package com.stagllc.staginfra.service;

import com.stagllc.staginfra.dto.RegistrationRequest;
import com.stagllc.staginfra.model.User;

public interface UserService {
    User registerUser(RegistrationRequest request);
    boolean verifyEmail(String token);
    boolean resendVerificationEmail(String email);
    User loginUser(String email, String password);
    void recordFailedLoginAttempt(String email);
    void resetFailedLoginAttempts(String email);
    String generateToken(User user);
    User updateUser(User user);
}