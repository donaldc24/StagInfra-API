package com.stagllc.staginfra.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stagllc.staginfra.config.TestSecurityConfig;
import com.stagllc.staginfra.dto.CostRequest;
import com.stagllc.staginfra.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WebMvcTest(CostController.class)
@Import(TestSecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class CostControllerTest {

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Reset test state
        CostRequest emptyRequest = new CostRequest();
        try {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/cost")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptyRequest)))
                    .andExpect(MockMvcResultMatchers.status().isOk());
        } catch (Exception e) {
            // Ignore exception during setup
        }
    }

    @Test
    void testGetCost_EmptyComponents_ReturnsZero() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/cost")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(content, Map.class);
        assertEquals(0.0, responseMap.get("total"));
    }

    @Test
    void testGetCost_SingleEC2_ReturnsCorrectCost() throws Exception {
        Map<String, Object> ec2 = new HashMap<>();
        ec2.put("type", "ec2");
        ec2.put("instances", 2);
        ec2.put("instance_type", "t2.micro");
        List<Map<String, Object>> components = Collections.singletonList(ec2);

        CostRequest costReq = new CostRequest();
        costReq.setComponents(components);

        MvcResult postResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/cost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(costReq)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String postContent = postResult.getResponse().getContentAsString();
        Map<String, Object> postResponseMap = objectMapper.readValue(postContent, Map.class);
        assertEquals("success", postResponseMap.get("status"));

        MvcResult getResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/cost")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String getContent = getResult.getResponse().getContentAsString();
        Map<String, Object> getResponseMap = objectMapper.readValue(getContent, Map.class);
        assertEquals(17.0, getResponseMap.get("total"));
    }

    @Test
    void testGetCost_SingleS3_ReturnsCorrectCost() throws Exception {
        Map<String, Object> s3 = new HashMap<>();
        s3.put("type", "s3");
        s3.put("storage", 50);
        List<Map<String, Object>> components = Collections.singletonList(s3);

        CostRequest costReq = new CostRequest();
        costReq.setComponents(components);

        MvcResult postResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/cost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(costReq)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String postContent = postResult.getResponse().getContentAsString();
        Map<String, Object> postResponseMap = objectMapper.readValue(postContent, Map.class);
        assertEquals("success", postResponseMap.get("status"));

        MvcResult getResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/cost")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String getContent = getResult.getResponse().getContentAsString();
        Map<String, Object> getResponseMap = objectMapper.readValue(getContent, Map.class);
        assertEquals(1.15, getResponseMap.get("total"));
    }

    @Test
    void testUpdateCost_NullComponents_SetsEmptyList() throws Exception {
        CostRequest costReq = new CostRequest();

        MvcResult postResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/cost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(costReq)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String postContent = postResult.getResponse().getContentAsString();
        Map<String, Object> postResponseMap = objectMapper.readValue(postContent, Map.class);
        assertEquals("success", postResponseMap.get("status"));

        MvcResult getResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/cost")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andReturn();

        String getContent = getResult.getResponse().getContentAsString();
        Map<String, Object> getResponseMap = objectMapper.readValue(getContent, Map.class);
        assertEquals(0.0, getResponseMap.get("total"));
    }
}