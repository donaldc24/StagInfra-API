// src/test/java/com/stagllc/staginfra/service/RateLimiterServiceTest.java
package com.stagllc.staginfra.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterServiceTest {

    @Test
    void testAllowRegistration() {
        RateLimiterService rateLimiter = new RateLimiterService();
        String testIp = "192.168.1.1";

        // First 5 attempts should be allowed
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.allowRegistration(testIp), "Attempt " + (i + 1) + " should be allowed");
        }

        // 6th attempt should be blocked
        assertFalse(rateLimiter.allowRegistration(testIp), "Attempt 6 should be blocked");
    }

    @Test
    void testAllowLogin() {
        RateLimiterService rateLimiter = new RateLimiterService();
        String testIp = "192.168.1.1";

        // First 5 attempts should be allowed
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.allowLogin(testIp), "Attempt " + (i + 1) + " should be allowed");
        }

        // 6th attempt should be blocked
        assertFalse(rateLimiter.allowLogin(testIp), "Attempt 6 should be blocked");
    }

    @Test
    void testDifferentIpAddressesAreIndependent() {
        RateLimiterService rateLimiter = new RateLimiterService();
        String ip1 = "192.168.1.1";
        String ip2 = "192.168.1.2";

        // Max out first IP's registration attempts
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.allowRegistration(ip1));
        }

        // First IP should be blocked
        assertFalse(rateLimiter.allowRegistration(ip1));

        // Second IP should still be allowed
        assertTrue(rateLimiter.allowRegistration(ip2));
    }

    @Test
    void testDifferentActionsAreIndependent() {
        RateLimiterService rateLimiter = new RateLimiterService();
        String ip = "192.168.1.1";

        // Max out IP's registration attempts
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.allowRegistration(ip));
        }

        // Registration should be blocked
        assertFalse(rateLimiter.allowRegistration(ip));

        // Login should still be allowed
        assertTrue(rateLimiter.allowLogin(ip));
    }

    @Test
    void testResetLimiter() {
        RateLimiterService rateLimiter = new RateLimiterService();
        String ip = "192.168.1.1";

        // Max out IP's registration attempts
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.allowRegistration(ip));
        }

        // Registration should be blocked
        assertFalse(rateLimiter.allowRegistration(ip));

        // Reset the limiter
        rateLimiter.resetLimiter(ip, "REGISTRATION");

        // Registration should be allowed again
        assertTrue(rateLimiter.allowRegistration(ip));
    }
}