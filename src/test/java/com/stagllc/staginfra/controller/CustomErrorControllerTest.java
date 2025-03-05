package com.stagllc.staginfra.controller;

import com.stagllc.staginfra.config.ControllerTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import jakarta.servlet.RequestDispatcher;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomErrorController.class)
@Import(ControllerTestConfig.class)
public class CustomErrorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomErrorController errorController;

    @Test
    public void testNotFoundError() throws Exception {
        // Create a mock request directly
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);

        // Call the controller method directly
        var response = errorController.handleError(request);

        // Assert on the response
        assertEquals(404, response.getStatusCode().value());
        assertEquals("{error=Not Found}", Objects.requireNonNull(response.getBody()).toString());
    }
}