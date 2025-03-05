package com.stagllc.staginfra.controller;

import com.stagllc.staginfra.dto.UserDTO;
import com.stagllc.staginfra.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserService userService;

    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UserDTO> getAllUsers() {
        logger.info("Admin request to get all users");
        return userService.getAllUsers();
    }

    @PostMapping(value = "/users/{userId}/verify", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDTO manuallyVerifyUser(@PathVariable Long userId) {
        logger.info("Admin request to manually verify user with ID: {}", userId);
        return userService.manuallyVerifyUser(userId);
    }

    @PostMapping(value = "/users/{userId}/admin", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDTO makeUserAdmin(@PathVariable Long userId) {
        logger.info("Admin request to grant ADMIN role to user with ID: {}", userId);
        boolean success = userService.makeUserAdmin(userId);

        if (success) {
            // Get updated user
            return userService.getUserById(userId);
        } else {
            return null; // This will result in a 200 OK with no content
        }
    }
}