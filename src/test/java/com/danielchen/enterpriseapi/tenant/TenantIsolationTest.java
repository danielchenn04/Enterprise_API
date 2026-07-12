package com.danielchen.enterpriseapi.tenant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Gated on DOCKER_API_COMPAT=true — see CLAUDE.md for OrbStack workaround
@EnabledIfEnvironmentVariable(named = "DOCKER_API_COMPAT", matches = "true")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TenantIsolationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    MockMvc mockMvc;

    @Test
    void tenantACannnotReadTenantBProjects() throws Exception {
        // --- Tenant A: sign up, create a project ---
        String tokenA = signupAndGetToken("Acme", "alice@acme.com", "password1");

        String createResp = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Acme Secret Project"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String projectId = com.jayway.jsonpath.JsonPath.read(createResp, "$.data.id");

        // --- Tenant B: sign up ---
        String tokenB = signupAndGetToken("Rival Corp", "bob@rival.com", "password2");

        // Tenant B tries to list — should see 0 projects (none from their org)
        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        // Tenant B tries to fetch Tenant A's project by ID — should get 404
        mockMvc.perform(get("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    private String signupAndGetToken(String orgName, String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orgName":"%s","email":"%s","password":"%s"}
                                """.formatted(orgName, email, password)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(body, "$.data.token");
    }
}
