package com.stagllc.staginfra.service;

import com.stagllc.staginfra.config.TestConfig;
import com.stagllc.staginfra.dto.RegistrationRequest;
import com.stagllc.staginfra.model.User;
import com.stagllc.staginfra.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = TestConfig.class)
@ActiveProfiles("test")
public class UserServiceTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @Autowired
    private UserService userService;

    private RegistrationRequest registrationRequest;
    private User user;

    @BeforeEach
    void setUp() {
        // Setup test registration request
        registrationRequest = new RegistrationRequest();
        registrationRequest.setEmail("test@example.com");
        registrationRequest.setPassword("Password123!");
        registrationRequest.setFirstName("John");
        registrationRequest.setLastName("Doe");
        registrationRequest.setCompany("Test Company");
        registrationRequest.setJobTitle("Developer");

        // Setup test user
        user = new User("test@example.com", "encodedPassword", "John", "Doe");
        user.setId(1L);
        user.setCompany("Test Company");
        user.setJobTitle("Developer");
    }

    @Test
    void testRegisterUserSuccess() {
        // Setup
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());

        // Execute
        User result = userService.registerUser(registrationRequest);

        // Verify
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("Test Company", result.getCompany());
        assertEquals("Developer", result.getJobTitle());

        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
        verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString());
    }

    @Test
    void testRegisterUserEmailAlreadyExists() {
        // Setup
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Execute & Verify
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(registrationRequest);
        });

        assertEquals("Email already in use", exception.getMessage());
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void testVerifyEmailSuccess() {
        // Setup
        String token = "valid-token";
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusDays(1));
        user.setEmailVerified(false);

        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Execute
        boolean result = userService.verifyEmail(token);

        // Verify
        assertTrue(result);
        assertTrue(user.isEmailVerified());
        assertNull(user.getVerificationToken());
        assertNull(user.getVerificationTokenExpiry());

        verify(userRepository).findByVerificationToken(token);
        verify(userRepository).save(user);
        verify(emailService).sendWelcomeEmail("test@example.com");
    }

    @Test
    void testVerifyEmailTokenExpired() {
        // Setup
        String token = "expired-token";
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(LocalDateTime.now().minusDays(1));
        user.setEmailVerified(false);

        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Execute
        boolean result = userService.verifyEmail(token);

        // Verify
        assertFalse(result);
        assertFalse(user.isEmailVerified());
        assertNotNull(user.getVerificationToken());
        assertNotEquals(token, user.getVerificationToken()); // New token generated
        assertNotNull(user.getVerificationTokenExpiry());

        verify(userRepository).findByVerificationToken(token);
        verify(userRepository).save(user);
        verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString());
    }

    @Test
    void testVerifyEmailTokenNotFound() {
        // Setup
        String token = "invalid-token";
        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.empty());

        // Execute
        boolean result = userService.verifyEmail(token);

        // Verify
        assertFalse(result);
        verify(userRepository).findByVerificationToken(token);
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendWelcomeEmail(anyString());
    }

    @Test
    void testResendVerificationEmailSuccess() {
        // Setup
        String email = "test@example.com";
        user.setEmailVerified(false);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Execute
        boolean result = userService.resendVerificationEmail(email);

        // Verify
        assertTrue(result);
        verify(userRepository).findByEmail(email);
        verify(userRepository).save(user);
        verify(emailService).sendVerificationEmail(eq(email), anyString());
    }

    @Test
    void testResendVerificationEmailUserAlreadyVerified() {
        // Setup
        String email = "test@example.com";
        user.setEmailVerified(true);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Execute
        boolean result = userService.resendVerificationEmail(email);

        // Verify
        assertFalse(result);
        verify(userRepository).findByEmail(email);
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void testResendVerificationEmailUserNotFound() {
        // Setup
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Execute
        boolean result = userService.resendVerificationEmail(email);

        // Verify
        assertFalse(result);
        verify(userRepository).findByEmail(email);
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }
}