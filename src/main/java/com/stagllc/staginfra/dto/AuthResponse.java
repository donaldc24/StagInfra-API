// src/main/java/com/stagllc/staginfra/dto/AuthResponse.java
package com.stagllc.staginfra.dto;

public class AuthResponse {
    private boolean success;
    private String message;
    private String token;
    private UserDTO user;

    // Constructors
    public AuthResponse() {
    }

    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AuthResponse(boolean success, String message, String token, UserDTO user) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.user = user;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    // Helper methods for common responses
    public static AuthResponse success(String message) {
        return new AuthResponse(true, message);
    }

    public static AuthResponse error(String message) {
        return new AuthResponse(false, message);
    }

    public static AuthResponse loginSuccess(String token, UserDTO user) {
        return new AuthResponse(true, "Login successful", token, user);
    }

    public static AuthResponse registrationSuccess() {
        return new AuthResponse(true, "Registration successful. Please check your email to verify your account.");
    }
}