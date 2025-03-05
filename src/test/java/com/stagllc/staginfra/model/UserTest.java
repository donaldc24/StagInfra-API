// src/test/java/com/stagllc/staginfra/model/UserTest.java
package com.stagllc.staginfra.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    void testUserCreation() {
        User user = new User("test@example.com", "password123", "John", "Doe");

        assertEquals("test@example.com", user.getEmail());
        assertEquals("password123", user.getPassword());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertFalse(user.isEmailVerified());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getVerificationToken());
        assertNotNull(user.getVerificationTokenExpiry());
    }

    @Test
    void testVerificationTokenGeneration() {
        User user = new User();
        assertNull(user.getVerificationToken());

        user.generateVerificationToken();

        assertNotNull(user.getVerificationToken());
        assertNotNull(user.getVerificationTokenExpiry());

        // Verify token expiry is set to roughly 48 hours in the future
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = user.getVerificationTokenExpiry();

        assertTrue(expiry.isAfter(now.plusDays(1).plusHours(23)));
        assertTrue(expiry.isBefore(now.plusDays(2).plusHours(1)));
    }

    @Test
    void testIsVerificationTokenValid() {
        User user = new User();

        // No token set
        assertFalse(user.isVerificationTokenValid());

        // Valid token
        user.generateVerificationToken();
        assertTrue(user.isVerificationTokenValid());

        // Expired token
        user.setVerificationTokenExpiry(LocalDateTime.now().minusDays(1));
        assertFalse(user.isVerificationTokenValid());
    }
}