// src/main/java/com/stagllc/staginfra/service/UserServiceImpl.java
package com.stagllc.staginfra.service;

import com.stagllc.staginfra.dto.RegistrationRequest;
import com.stagllc.staginfra.model.User;
import com.stagllc.staginfra.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

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
        Optional<User> userOpt = userRepository.findByVerificationToken(token);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (user.isVerificationTokenValid()) {
                user.setEmailVerified(true);
                user.setVerificationToken(null);
                user.setVerificationTokenExpiry(null);
                userRepository.save(user);

                // Send welcome email
                emailService.sendWelcomeEmail(user.getEmail());

                logger.info("Email verified for user: {}", user.getEmail());
                return true;
            } else {
                logger.warn("Expired verification token used for user: {}", user.getEmail());
                // Token expired, generate a new one
                user.generateVerificationToken();
                userRepository.save(user);
                emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());
            }
        } else {
            logger.warn("Invalid verification token used: {}", token);
        }

        return false;
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
}