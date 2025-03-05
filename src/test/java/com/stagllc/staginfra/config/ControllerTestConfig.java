// src/test/java/com/stagllc/staginfra/config/ControllerTestConfig.java
package com.stagllc.staginfra.config;

import com.stagllc.staginfra.security.JwtAuthenticationFilter;
import com.stagllc.staginfra.service.JwtService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import(TestSecurityConfig.class)
public class ControllerTestConfig {

    @MockBean
    public JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    public JwtService jwtService;
}