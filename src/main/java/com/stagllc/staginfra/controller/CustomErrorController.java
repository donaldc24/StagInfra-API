package com.stagllc.staginfra.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
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
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status != null && Integer.valueOf(status.toString()) == HttpStatus.NOT_FOUND.value()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Not Found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        // Fallback for other errors (not required for Ticket 1, but extensible)
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}