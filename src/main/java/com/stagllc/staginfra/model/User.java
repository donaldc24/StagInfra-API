package com.stagllc.staginfra.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column
    private String company;

    @Column
    private String jobTitle;

    @Column(nullable = false)
    private boolean emailVerified;

    @Column
    private String verificationToken;

    @Column
    private LocalDateTime verificationTokenExpiry;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastLogin;

    @Column
    private int failedLoginAttempts;

    @Column
    private LocalDateTime lockedUntil;

    @Column
    private String lastSuccessfulToken;

    @Column
    private boolean isAdmin;

    @Column
    private String roles;

    @Column(columnDefinition = "TEXT")
    private String activeSessions; // Will store JSON array of session tokens

    // Add getter and setter methods
    public String getActiveSessions() {
        return activeSessions == null ? "[]" : activeSessions;
    }

    public void setActiveSessions(String activeSessions) {
        this.activeSessions = activeSessions;
    }

    // Helper methods to manage sessions
    public List<String> getActiveSessionsList() {
        try {
            return new ObjectMapper().readValue(getActiveSessions(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void addSession(String token) {
        List<String> sessions = getActiveSessionsList();
        if (!sessions.contains(token)) {
            sessions.add(token);
            try {
                this.activeSessions = new ObjectMapper().writeValueAsString(sessions);
            } catch (Exception e) {
                throw new RuntimeException("Error storing session", e);
            }
        }
    }

    public void removeSession(String token) {
        List<String> sessions = getActiveSessionsList();
        if (sessions.remove(token)) {
            try {
                this.activeSessions = new ObjectMapper().writeValueAsString(sessions);
            } catch (Exception e) {
                throw new RuntimeException("Error removing session", e);
            }
        }
    }

    public void clearAllSessions() {
        this.activeSessions = "[]";
    }

    public String getRoles() {
        return roles == null ? "" : roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public List<String> getRolesList() {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(roles.split(","));
    }

    public boolean hasRole(String role) {
        return getRolesList().contains(role);
    }

    public void addRole(String role) {
        List<String> currentRoles = new ArrayList<>(getRolesList());
        if (!currentRoles.contains(role)) {
            currentRoles.add(role);
            this.roles = String.join(",", currentRoles);
        }
    }

    public void removeRole(String role) {
        List<String> currentRoles = new ArrayList<>(getRolesList());
        if (currentRoles.remove(role)) {
            this.roles = String.join(",", currentRoles);
        }
    }

    // Add getter and setter
    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    // Add getter and setter
    public String getLastSuccessfulToken() {
        return lastSuccessfulToken;
    }

    public void setLastSuccessfulToken(String lastSuccessfulToken) {
        this.lastSuccessfulToken = lastSuccessfulToken;
    }

    // Default constructor required by JPA
    public User() {
        this.createdAt = LocalDateTime.now();
        this.emailVerified = false;
        this.failedLoginAttempts = 0;
    }

    public User(String email, String password, String firstName, String lastName) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAt = LocalDateTime.now();
        this.emailVerified = false;
        this.failedLoginAttempts = 0;
        this.generateVerificationToken();
    }

    public void generateVerificationToken() {
        this.verificationToken = UUID.randomUUID().toString();
        this.verificationTokenExpiry = LocalDateTime.now().plusDays(2); // 48 hours validity
    }

    public boolean isVerificationTokenValid() {
        return verificationToken != null &&
                verificationTokenExpiry != null &&
                LocalDateTime.now().isBefore(verificationTokenExpiry);
    }

    public boolean isAccountLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;

        // Lock account after 5 failed attempts
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(15); // Lock for 15 minutes
        }
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public LocalDateTime getVerificationTokenExpiry() {
        return verificationTokenExpiry;
    }

    public void setVerificationTokenExpiry(LocalDateTime verificationTokenExpiry) {
        this.verificationTokenExpiry = verificationTokenExpiry;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }
}