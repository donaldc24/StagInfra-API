package com.stagllc.staginfra.config;

import com.stagllc.staginfra.controller.CostController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest({CorsConfig.class, CostController.class})
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCorsMappings() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.options("/api/cost")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"))
                .andExpect(MockMvcResultMatchers.header().string("Access-Control-Allow-Credentials", "true"));
    }
}