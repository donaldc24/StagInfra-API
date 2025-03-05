package com.stagllc.staginfra.config;

import com.stagllc.staginfra.controller.CostController;
import com.stagllc.staginfra.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest({CorsConfig.class, CostController.class})
@Import(TestSecurityConfig.class)
public class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void testCorsMappings() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.options("/api/cost")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}