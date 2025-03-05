package com.stagllc.staginfra.controller;

import com.stagllc.staginfra.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(CustomErrorController.class)
@Import(TestSecurityConfig.class)
public class CustomErrorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HttpServletRequest request;

    @Test
    public void testNotFoundError() throws Exception {
        // Mock the request to simulate a 404 error
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(HttpStatus.NOT_FOUND.value());

        mockMvc.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.NOT_FOUND.value()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}