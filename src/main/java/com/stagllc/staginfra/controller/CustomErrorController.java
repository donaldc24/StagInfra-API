package com.stagllc.staginfra.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<Map<String, String>> handleError(HttpServletRequest request) {
        // Get error attributes from the request
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = (statusObj != null) ? Integer.parseInt(statusObj.toString()) : 500;

        Map<String, String> response = new HashMap<>();

        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            response.put("error", "Not Found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // Default error response
        response.put("error", "Internal Server Error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}