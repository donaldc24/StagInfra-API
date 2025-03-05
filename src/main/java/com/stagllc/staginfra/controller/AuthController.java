package com.stagllc.staginfra.controller;

import com.stagllc.staginfra.dto.AuthResponse;
import com.stagllc.staginfra.dto.LoginRequest;
import com.stagllc.staginfra.dto.RegistrationRequest;
import com.stagllc.staginfra.dto.UserDTO;
import com.stagllc.staginfra.model.User;
import com.stagllc.staginfra.service.RateLimiterService;
import com.stagllc.staginfra.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final Map<String, Boolean> recentVerifications = new ConcurrentHashMap<>();

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

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        // Get client IP address for rate limiting
        String clientIp = getClientIp(httpRequest);

        // Check rate limits
        if (!rateLimiterService.allowLogin(clientIp)) {
            logger.warn("Login rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(429)
                    .body(AuthResponse.error("Too many login attempts. Please try again later."));
        }

        try {
            // Attempt to login
            User user = userService.loginUser(request.getEmail(), request.getPassword());

            if (user != null && passwordMatches) {
                // Generate JWT token
                String token = userService.generateToken(user);
                String refreshToken = userService.generateRefreshToken(user);

                // Store the token in the user's active sessions
                user.addSession(token);
                user.setLastSuccessfulToken(refreshToken);
                userRepository.save(user);

                // Return successful response
                UserDTO userDto = UserDTO.fromUser(user);
                return ResponseEntity.ok(new AuthResponse(true, "Login successful", token, refreshToken, userDto));
            }

            if (user == null) {
                // Reset rate limiter on successful login
                rateLimiterService.resetLimiter(clientIp, "LOGIN");

                // Increment failed login attempts
                userService.recordFailedLoginAttempt(request.getEmail());

                return ResponseEntity.badRequest()
                        .body(AuthResponse.error("Invalid email or password"));
            }

            // Check if email is verified
            if (!user.isEmailVerified()) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.error("Please verify your email address before logging in"));
            }

            // Generate JWT token
//            String token = userService.generateToken(user);

            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", user.getRolesList());
            String token = userService.generateToken(claims, user);

            // Update last login time
            user.setLastLogin(LocalDateTime.now());
            userService.updateUser(user);

            // Reset rate limiter on successful login
            rateLimiterService.resetLimiter(clientIp, "LOGIN");

            // Reset failed login attempts
            userService.resetFailedLoginAttempts(user.getEmail());

            // Convert User to UserDTO for response
            UserDTO userDto = UserDTO.fromUser(user);

            logger.info("User logged in successfully: {}", user.getEmail());
            return ResponseEntity.ok(AuthResponse.loginSuccess(token, userDto));

        } catch (Exception e) {
            logger.error("Login error", e);
            return ResponseEntity.badRequest()
                    .body(AuthResponse.error("An error occurred during login"));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestParam String token) {
        logger.info("Received verification request for token: {}", token);

        try {
            boolean verified = userService.verifyEmail(token);

            if (verified) {
                logger.info("Email verification successful for token: {}", token);
                return ResponseEntity.ok(AuthResponse.success("Email verified successfully"));
            } else {
                logger.warn("Email verification failed for token: {}", token);
                return ResponseEntity.ok(AuthResponse.error("Invalid or expired verification token"));
            }
        } catch (Exception e) {
            logger.error("Error during email verification", e);
            return ResponseEntity.ok(AuthResponse.error("An error occurred during verification: " + e.getMessage()));
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

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestParam String refreshToken) {
        try {
            // Validate refresh token
            String userEmail = jwtService.extractUsername(refreshToken);
            if (userEmail == null) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.error("Invalid refresh token"));
            }

            // Find the user
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.error("User not found"));
            }

            User user = userOpt.get();

            // Verify the refresh token hasn't been revoked
            if (!user.getLastSuccessfulToken().equals(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.error("Refresh token has been revoked"));
            }

            // Generate new tokens if the refresh token is valid
            if (!jwtService.isTokenExpired(refreshToken)) {
                // Generate new access token
                String newToken = jwtService.generateToken(user);
                String newRefreshToken = jwtService.generateRefreshToken(user);

                // Update user's refresh token
                user.setLastSuccessfulToken(newRefreshToken);
                userRepository.save(user);

                // Return the new tokens
                UserDTO userDto = UserDTO.fromUser(user);
                return ResponseEntity.ok(new AuthResponse(true, "Token refreshed successfully", newToken, userDto));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponse.error("Refresh token expired"));
            }
        } catch (Exception e) {
            logger.error("Error refreshing token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.error("Error refreshing token"));
        }
    }

    @GetMapping("/active-sessions")
    public ResponseEntity<Map<String, Object>> getActiveSessions(
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract token
        String token = authHeader.substring(7);
        String userEmail = jwtService.extractUsername(token);

        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Get user and their sessions
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userOpt.get();
        List<String> sessions = user.getActiveSessionsList();

        // Decode session info for display
        List<Map<String, Object>> sessionInfo = new ArrayList<>();
        for (String sessionToken : sessions) {
            try {
                // Extract session info from token
                Claims claims = jwtService.extractAllClaims(sessionToken);
                Date issuedAt = claims.getIssuedAt();
                Date expiration = claims.getExpiration();

                // Create session info object
                Map<String, Object> session = new HashMap<>();
                session.put("token", sessionToken);
                session.put("issuedAt", issuedAt.toString());
                session.put("expiration", expiration.toString());
                session.put("isCurrent", sessionToken.equals(token));

                sessionInfo.add(session);
            } catch (Exception e) {
                // Skip invalid tokens
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("sessions", sessionInfo);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout-all-devices")
    public ResponseEntity<AuthResponse> logoutAllDevices(
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract token
        String token = authHeader.substring(7);
        String userEmail = jwtService.extractUsername(token);

        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Get user and clear all sessions
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userOpt.get();
        user.clearAllSessions();
        userRepository.save(user);

        return ResponseEntity.ok(AuthResponse.success("Logged out from all devices"));
    }

    @PostMapping("/logout-device")
    public ResponseEntity<AuthResponse> logoutDevice(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String sessionToken) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract token
        String token = authHeader.substring(7);
        String userEmail = jwtService.extractUsername(token);

        if (userEmail == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Get user and remove specific session
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userOpt.get();
        user.removeSession(sessionToken);
        userRepository.save(user);

        return ResponseEntity.ok(AuthResponse.success("Device logged out"));
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