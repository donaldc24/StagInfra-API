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

    @Test
    void testRoles() {
        // Setup
        User user = new User();

        // Empty roles
        assertEquals(0, user.getRolesList().size());

        // Add roles
        user.addRole("USER");
        assertEquals(1, user.getRolesList().size());
        assertTrue(user.hasRole("USER"));

        // Add duplicate role
        user.addRole("USER");
        assertEquals(1, user.getRolesList().size());

        // Add another role
        user.addRole("ADMIN");
        assertEquals(2, user.getRolesList().size());
        assertTrue(user.hasRole("ADMIN"));

        // Remove role
        user.removeRole("USER");
        assertEquals(1, user.getRolesList().size());
        assertFalse(user.hasRole("USER"));
        assertTrue(user.hasRole("ADMIN"));

        // Set roles directly
        user.setRoles("EDITOR,VIEWER");
        assertEquals(2, user.getRolesList().size());
        assertTrue(user.hasRole("EDITOR"));
        assertTrue(user.hasRole("VIEWER"));
        assertFalse(user.hasRole("ADMIN"));
    }
}