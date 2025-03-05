package com.stagllc.staginfra.controller;

import com.stagllc.staginfra.dto.UserDTO;
import com.stagllc.staginfra.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        logger.info("Admin request to get all users");
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/users/{userId}/verify")
    public ResponseEntity<UserDTO> manuallyVerifyUser(@PathVariable Long userId) {
        logger.info("Admin request to manually verify user with ID: {}", userId);
        UserDTO verifiedUser = userService.manuallyVerifyUser(userId);
        return ResponseEntity.ok(verifiedUser);
    }

    @PostMapping("/users/{userId}/admin")
    public ResponseEntity<UserDTO> makeUserAdmin(@PathVariable Long userId) {
        logger.info("Admin request to grant ADMIN role to user with ID: {}", userId);
        boolean success = userService.makeUserAdmin(userId);

        if (success) {
            // Get updated user
            UserDTO updatedUser = userService.getUserById(userId);
            return ResponseEntity.ok(updatedUser);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
}