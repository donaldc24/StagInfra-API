// src/main/java/com/stagllc/staginfra/service/RateLimiterService.java
package com.stagllc.staginfra.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    // Store IP address and attempt timestamps
    private final Map<String, RequestLog> requestLogs = new ConcurrentHashMap<>();

    // Config
    private static final int MAX_REGISTRATION_ATTEMPTS = 5;
    private static final int REGISTRATION_WINDOW_MINUTES = 60;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOGIN_WINDOW_MINUTES = 15;

    public boolean allowRegistration(String ipAddress) {
        return checkRateLimit(ipAddress, "REGISTRATION", MAX_REGISTRATION_ATTEMPTS, REGISTRATION_WINDOW_MINUTES);
    }

    public boolean allowLogin(String ipAddress) {
        return checkRateLimit(ipAddress, "LOGIN", MAX_LOGIN_ATTEMPTS, LOGIN_WINDOW_MINUTES);
    }

    private boolean checkRateLimit(String ipAddress, String actionType, int maxAttempts, int windowMinutes) {
        String key = ipAddress + ":" + actionType;
        RequestLog log = requestLogs.computeIfAbsent(key, k -> new RequestLog());

        // Clean old attempts
        log.cleanOldAttempts(windowMinutes);

        // Check if under limit
        if (log.getRecentAttempts(windowMinutes) < maxAttempts) {
            // Record this attempt
            log.recordAttempt();
            return true;
        }

        return false;
    }

    // Reset rate limit for an IP (e.g., after successful login)
    public void resetLimiter(String ipAddress, String actionType) {
        String key = ipAddress + ":" + actionType;
        requestLogs.remove(key);
    }

    // Inner class to track request attempts
    private static class RequestLog {
        private final Map<LocalDateTime, Integer> attempts = new ConcurrentHashMap<>();

        public void recordAttempt() {
            LocalDateTime now = LocalDateTime.now();
            attempts.put(now, attempts.getOrDefault(now, 0) + 1);
        }

        public void cleanOldAttempts(int windowMinutes) {
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(windowMinutes);
            attempts.keySet().removeIf(timestamp -> timestamp.isBefore(cutoff));
        }

        public int getRecentAttempts(int windowMinutes) {
            cleanOldAttempts(windowMinutes);
            return attempts.values().stream().mapToInt(Integer::intValue).sum();
        }
    }
}