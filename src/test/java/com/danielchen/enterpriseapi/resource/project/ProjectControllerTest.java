package com.danielchen.enterpriseapi.resource.project;

import com.danielchen.enterpriseapi.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProjectControllerTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        String email = "projtest-" + UUID.randomUUID() + "@example.com";
        String resp = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orgName":"Proj Org","email":"%s","password":"Password1!"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        adminToken = JsonPath.read(resp, "$.data.token");
    }

    @Test
    void createAndGetProject() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Alpha", "description": "First project"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Alpha"));

        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Alpha"));
    }

    @Test
    void returns404ForMissingProject() throws Exception {
        mockMvc.perform(get("/api/v1/projects/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void returnsValidationErrorForBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
