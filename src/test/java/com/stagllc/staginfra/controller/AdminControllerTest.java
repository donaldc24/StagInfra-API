package com.stagllc.staginfra.controller;

import com.stagllc.staginfra.config.ControllerTestConfig;
import com.stagllc.staginfra.dto.UserDTO;
import com.stagllc.staginfra.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(ControllerTestConfig.class)
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private UserDTO testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserDTO();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmailVerified(true);
        testUser.setRoles(Collections.singletonList("ADMIN"));

        // Setup mocks
        List<UserDTO> users = Arrays.asList(testUser);
        when(userService.getAllUsers()).thenReturn(users);
        when(userService.manuallyVerifyUser(anyLong())).thenReturn(testUser);
        when(userService.makeUserAdmin(anyLong())).thenReturn(true);
        when(userService.getUserById(anyLong())).thenReturn(testUser);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllUsers() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andReturn();

        // Debug output
        System.out.println("RESPONSE CONTENT TYPE: " + result.getResponse().getContentType());
        System.out.println("RESPONSE BODY: " + result.getResponse().getContentAsString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testManuallyVerifyUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/users/1/verify")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andReturn();

        // Debug output
        System.out.println("RESPONSE CONTENT TYPE: " + result.getResponse().getContentType());
        System.out.println("RESPONSE BODY: " + result.getResponse().getContentAsString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testMakeUserAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admin/users/1/admin")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andReturn();

        // Debug output
        System.out.println("RESPONSE CONTENT TYPE: " + result.getResponse().getContentType());
        System.out.println("RESPONSE BODY: " + result.getResponse().getContentAsString());
    }
}