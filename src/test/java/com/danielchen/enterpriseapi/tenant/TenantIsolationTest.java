package com.danielchen.enterpriseapi.tenant;

import com.danielchen.enterpriseapi.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Gated on DOCKER_API_COMPAT=true — see AbstractIntegrationTest
class TenantIsolationTest extends AbstractIntegrationTest {

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

        String projectId = JsonPath.read(createResp, "$.data.id");

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

        return JsonPath.read(body, "$.data.token");
    }
}
