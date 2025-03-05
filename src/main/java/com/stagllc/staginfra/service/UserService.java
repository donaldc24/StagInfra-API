package com.stagllc.staginfra.service;

import com.stagllc.staginfra.dto.RegistrationRequest;
import com.stagllc.staginfra.dto.UserDTO;
import com.stagllc.staginfra.model.User;

import java.util.List;
import java.util.Map;

public interface UserService {
    User registerUser(RegistrationRequest request);
    boolean verifyEmail(String token);
    boolean resendVerificationEmail(String email);
    User loginUser(String email, String password);
    void recordFailedLoginAttempt(String email);
    void resetFailedLoginAttempts(String email);
    String generateToken(User user);
    String generateToken(Map<String, Object> extraClaims, User user); // Added this method
    User updateUser(User user);

    // Admin functionality
    List<UserDTO> getAllUsers();
    UserDTO manuallyVerifyUser(Long userId);
    boolean makeUserAdmin(Long userId);
    UserDTO getUserById(Long userId);
}