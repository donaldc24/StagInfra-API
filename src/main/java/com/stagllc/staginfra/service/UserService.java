// src/main/java/com/stagllc/staginfra/service/UserService.java
package com.stagllc.staginfra.service;

import com.stagllc.staginfra.dto.RegistrationRequest;
import com.stagllc.staginfra.model.User;

public interface UserService {
    User registerUser(RegistrationRequest request);
    boolean verifyEmail(String token);
    boolean resendVerificationEmail(String email);
}