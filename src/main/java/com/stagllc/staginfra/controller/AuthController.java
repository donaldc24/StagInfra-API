// Update src/main/java/com/stagllc/staginfra/controller/AuthController.java
package com.stagllc.staginfra.controller;

import com.stagllc.staginfra.dto.AuthResponse;
import com.stagllc.staginfra.dto.RegistrationRequest;
import com.stagllc.staginfra.model.User;
import com.stagllc.staginfra.service.RateLimiterService;
import com.stagllc.staginfra.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private RateLimiterService rateLimiterService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(
            @Valid @RequestBody RegistrationRequest request,
            HttpServletRequest httpRequest) {

        // Get client IP address for rate limiting
        String clientIp = getClientIp(httpRequest);

        // Check rate limits
        if (!rateLimiterService.allowRegistration(clientIp)) {
            logger.warn("Registration rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(429)
                    .body(AuthResponse.error("Too many registration attempts. Please try again later."));
        }

        try {
            User user = userService.registerUser(request);

            logger.info("User registered successfully: {}", user.getEmail());
            return ResponseEntity.ok(AuthResponse.registrationSuccess());
        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(AuthResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during registration", e);
            return ResponseEntity.badRequest().body(AuthResponse.error("Registration failed: " + e.getMessage()));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestParam String token) {
        boolean verified = userService.verifyEmail(token);

        if (verified) {
            return ResponseEntity.ok(AuthResponse.success("Email verified successfully"));
        } else {
            return ResponseEntity.badRequest().body(AuthResponse.error("Invalid or expired verification token"));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<AuthResponse> resendVerificationEmail(
            @RequestParam String email,
            HttpServletRequest httpRequest) {

        // Get client IP address for rate limiting
        String clientIp = getClientIp(httpRequest);

        // Check rate limits
        if (!rateLimiterService.allowRegistration(clientIp)) {
            logger.warn("Verification email rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(429)
                    .body(AuthResponse.error("Too many requests. Please try again later."));
        }

        boolean sent = userService.resendVerificationEmail(email);

        if (sent) {
            return ResponseEntity.ok(AuthResponse.success("Verification email resent successfully"));
        } else {
            return ResponseEntity.badRequest().body(AuthResponse.error("Failed to resend verification email"));
        }
    }

    // Helper method to get client IP address
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Get first IP in case of proxies
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}