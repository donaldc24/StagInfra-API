package com.stagllc.staginfra.util;

import com.stagllc.staginfra.model.User;
import com.stagllc.staginfra.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTestUtil {

    @Autowired
    private JwtService jwtService;

    public String generateToken(User user) {
        return jwtService.generateToken(user);
    }

    public String generateTokenWithClaims(User user, String... roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        return jwtService.generateToken(claims, user);
    }
}