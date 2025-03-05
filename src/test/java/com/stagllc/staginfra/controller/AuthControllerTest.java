package com.stagllc.staginfra.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stagllc.staginfra.dto.LoginRequest;
import com.stagllc.staginfra.dto.RegistrationRequest;
import com.stagllc.staginfra.dto.UserDTO;
import com.stagllc.staginfra.model.User;
import com.stagllc.staginfra.repository.UserRepository;
import com.stagllc.staginfra.service.JwtService;
import com.stagllc.staginfra.service.RateLimiterService;
import com.stagllc.staginfra.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserRepository userRepository;

    private RegistrationRequest validRequest;
    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        validRequest = new RegistrationRequest();
        validRequest.setEmail("test@example.com");
        validRequest.setPassword("Password123!");
        validRequest.setFirstName("John");
        validRequest.setLastName("Doe");

        testUser = new User(
                validRequest.getEmail(),
                "encodedPassword",
                validRequest.getFirstName(),
                validRequest.getLastName()
        );
        testUser.setId(1L);
        testUser.setEmailVerified(true);

        testUserDTO = new UserDTO();
        testUserDTO.setId(1L);
        testUserDTO.setEmail(testUser.getEmail());
        testUserDTO.setFirstName(testUser.getFirstName());
        testUserDTO.setLastName(testUser.getLastName());
        testUserDTO.setEmailVerified(true);
        testUserDTO.setRoles(Collections.emptyList());

        // Setup mocks for UserRepository
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
    }

    @Test
    void testRegisterUserSuccess() throws Exception {
        // Setup
        when(rateLimiterService.allowRegistration(anyString())).thenReturn(true);
        when(userService.registerUser(any(RegistrationRequest.class))).thenReturn(testUser);

        // Execute & Verify
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful. Please check your email to verify your account."));
    }

    @Test
    void testRegisterUserRateLimitExceeded() throws Exception {
        // Setup
        when(rateLimiterService.allowRegistration(anyString())).thenReturn(false);

        // Execute & Verify
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Too many registration attempts. Please try again later."));
    }

    @Test
    void testRegisterUserEmailAlreadyExists() throws Exception {
        // Setup
        when(rateLimiterService.allowRegistration(anyString())).thenReturn(true);
        when(userService.registerUser(any(RegistrationRequest.class)))
                .thenThrow(new IllegalArgumentException("Email already in use"));

        // Execute & Verify
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email already in use"));
    }

    @Test
    void testVerifyEmailSuccess() throws Exception {
        // Setup
        String token = "valid-token";
        when(userService.verifyEmail(token)).thenReturn(true);

        // Execute & Verify
        mockMvc.perform(get("/api/auth/verify")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"));
    }

    @Test
    void testVerifyEmailInvalid() throws Exception {
        // Setup
        String token = "invalid-token";
        when(userService.verifyEmail(token)).thenReturn(false);

        // Execute & Verify
        mockMvc.perform(get("/api/auth/verify")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired verification token"));
    }

    @Test
    void testResendVerificationEmailSuccess() throws Exception {
        // Setup
        String email = "test@example.com";
        when(rateLimiterService.allowRegistration(anyString())).thenReturn(true);
        when(userService.resendVerificationEmail(email)).thenReturn(true);

        // Execute & Verify
        mockMvc.perform(post("/api/auth/resend-verification")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Verification email resent successfully"));
    }

    @Test
    void testResendVerificationEmailRateLimitExceeded() throws Exception {
        // Setup
        String email = "test@example.com";
        when(rateLimiterService.allowRegistration(anyString())).thenReturn(false);

        // Execute & Verify
        mockMvc.perform(post("/api/auth/resend-verification")
                        .param("email", email))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."));
    }

    @Test
    void testResendVerificationEmailFailed() throws Exception {
        // Setup
        String email = "test@example.com";
        when(rateLimiterService.allowRegistration(anyString())).thenReturn(true);
        when(userService.resendVerificationEmail(email)).thenReturn(false);

        // Execute & Verify
        mockMvc.perform(post("/api/auth/resend-verification")
                        .param("email", email))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Failed to resend verification email"));
    }

    @Test
    void testLoginUserSuccess() throws Exception {
        // Setup
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        when(rateLimiterService.allowLogin(anyString())).thenReturn(true);
        when(userService.loginUser(anyString(), anyString())).thenReturn(testUser);
        when(userService.generateToken(any(), any())).thenReturn("dummy-token");

        // Execute & Verify
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").exists());
    }
}