// src/main/java/com/stagllc/staginfra/service/UserServiceImpl.java
package com.stagllc.staginfra.service;

import com.stagllc.staginfra.dto.RegistrationRequest;
import com.stagllc.staginfra.dto.UserDTO;
import com.stagllc.staginfra.model.User;
import com.stagllc.staginfra.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public User registerUser(RegistrationRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        // Create new user
        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName()
        );

        user.setCompany(request.getCompany());
        user.setJobTitle(request.getJobTitle());

        // Save user to get the ID
        User savedUser = userRepository.save(user);

        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());

        // Log the registration
        logger.info("New user registered: {}", user.getEmail());

        return savedUser;
    }

    @Override
    @Transactional
    public boolean verifyEmail(String token) {
        logger.info("Verifying email with token: {}", token);

        if (token == null || token.isBlank()) {
            logger.warn("Empty or null verification token");
            return false;
        }

        // First try to find by current token
        Optional<User> userOpt = userRepository.findByVerificationToken(token);

        if (userOpt.isEmpty()) {
            logger.warn("No user found with verification token: {}", token);
            return false;
        }

        User user = userOpt.get();
        logger.info("Found user for verification: {}", user.getEmail());

        // If user is already verified, return true (idempotent operation)
        if (user.isEmailVerified()) {
            logger.info("User already verified: {}", user.getEmail());
            return true;
        }

        if (user.isVerificationTokenValid()) {
            try {
                user.setEmailVerified(true);
                // Don't clear the token immediately to allow for duplicate requests
                // We'll keep the token but mark the account as verified
                userRepository.save(user);

                // Send welcome email
                try {
                    emailService.sendWelcomeEmail(user.getEmail());
                } catch (Exception e) {
                    logger.error("Failed to send welcome email to {}", user.getEmail(), e);
                    // Don't fail the verification if the welcome email fails
                }

                logger.info("Email verified successfully for user: {}", user.getEmail());
                return true;
            } catch (Exception e) {
                logger.error("Error saving user after verification", e);
                throw e;
            }
        } else {
            logger.warn("Expired verification token used for user: {}", user.getEmail());
            // Token expired, generate a new one
            try {
                user.generateVerificationToken();
                userRepository.save(user);
                emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());
                logger.info("Generated and sent new verification token for user: {}", user.getEmail());
            } catch (Exception e) {
                logger.error("Error generating new verification token", e);
            }
            return false;
        }
    }

    @Override
    public boolean resendVerificationEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (!user.isEmailVerified()) {
                // Generate a new token
                user.generateVerificationToken();
                userRepository.save(user);

                // Send the email
                emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());

                logger.info("Verification email resent to: {}", email);
                return true;
            } else {
                logger.info("Verification email requested for already verified user: {}", email);
            }
        } else {
            logger.warn("Verification email requested for unknown user: {}", email);
        }

        return false;
    }

    @Override
    @Transactional
    public User loginUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            logger.warn("Login attempt for non-existing user: {}", email);
            return null;
        }

        User user = userOpt.get();

        // Check if account is locked
        if (user.isAccountLocked()) {
            logger.warn("Login attempt for locked account: {}", email);
            return null;
        }

        // Verify password
        boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());

        if (!passwordMatches) {
            logger.warn("Invalid password for user: {}", email);
            return null;
        }

        return user;
    }

    @Override
    @Transactional
    public void recordFailedLoginAttempt(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public void resetFailedLoginAttempts(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.resetFailedLoginAttempts();
            userRepository.save(user);
        });
    }

    @Override
    public String generateToken(User user) {
        return jwtService.generateToken(user);
    }

    @Override
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(UserDTO::fromUser)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDTO manuallyVerifyUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);

            try {
                emailService.sendWelcomeEmail(user.getEmail());
            } catch (Exception e) {
                logger.error("Failed to send welcome email to {}", user.getEmail(), e);
                // Don't fail the verification if the welcome email fails
            }

            logger.info("User {} manually verified by admin", user.getEmail());
        }

        return UserDTO.fromUser(user);
    }

    @Override
    @Transactional
    public boolean makeUserAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        user.addRole("ADMIN");
        userRepository.save(user);

        logger.info("User {} granted admin role", user.getEmail());
        return true;
    }

    @Override
    public UserDTO getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        return UserDTO.fromUser(user);
    }

    @PostConstruct
    @Transactional
    public void initializeAdmin() {
        // Check if admin exists
        Optional<User> adminUser = userRepository.findByEmail("admin@staginfra.com");

        if (adminUser.isEmpty()) {
            // Create admin user
            User admin = new User(
                    "admin@staginfra.com",
                    passwordEncoder.encode("AdminPass123!"),
                    "System",
                    "Admin"
            );
            admin.setEmailVerified(true);
            admin.addRole("ADMIN");
            userRepository.save(admin);

            logger.info("Admin user created: admin@staginfra.com");
        }
    }

    @Override
    public String generateToken(Map<String, Object> extraClaims, User user) {
        return jwtService.generateToken(extraClaims, user);
    }
}