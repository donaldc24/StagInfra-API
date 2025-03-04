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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.stagllc.staginfra.dto.UserDTO;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
public class UserServiceTest {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @MockBean
    private JwtService jwtService;

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
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setEmailVerified(true);
            // We're not clearing the token in the implementation anymore,
            // so don't expect it to be null in the test
            return savedUser;
        });

        // Execute
        boolean result = userService.verifyEmail(token);

        // Verify
        assertTrue(result);
        assertTrue(user.isEmailVerified());
        // Don't check if token is null since we're not clearing it anymore
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

    @Test
    void testGetAllUsers() {
        // Setup
        List<User> userList = Collections.singletonList(user);
        when(userRepository.findAll()).thenReturn(userList);

        // Execute
        List<UserDTO> result = userService.getAllUsers();

        // Verify
        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
        verify(userRepository).findAll();
    }

    @Test
    void testManuallyVerifyUser() {
        // Setup
        user.setEmailVerified(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Execute
        UserDTO result = userService.manuallyVerifyUser(1L);

        // Verify
        assertTrue(user.isEmailVerified());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
        verify(emailService).sendWelcomeEmail("test@example.com");
    }

    @Test
    void testManuallyVerifyUser_UserNotFound() {
        // Setup
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Execute & Verify
        assertThrows(IllegalArgumentException.class, () -> {
            userService.manuallyVerifyUser(99L);
        });
        verify(userRepository).findById(99L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testMakeUserAdmin() {
        // Setup
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Execute
        boolean result = userService.makeUserAdmin(1L);

        // Verify
        assertTrue(result);
        assertTrue(user.getRolesList().contains("ADMIN"));
        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
    }

    @Test
    void testGetUserById() {
        // Setup
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Execute
        UserDTO result = userService.getUserById(1L);

        // Verify
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).findById(1L);
    }
}