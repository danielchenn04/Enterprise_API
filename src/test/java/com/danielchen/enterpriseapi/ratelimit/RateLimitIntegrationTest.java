package com.danielchen.enterpriseapi.ratelimit;

import com.danielchen.enterpriseapi.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RateLimitIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void burstingOneTenantDoesNotAffectAnother() throws Exception {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String tokenA = signupAndGetToken("RateOrg-A-" + id, "rla-" + id + "@example.com", "Password1!");
        String tokenB = signupAndGetToken("RateOrg-B-" + id, "rlb-" + id + "@example.com", "Password1!");

        // Exhaust Tenant A's 100-req/min FREE bucket.
        // Burst 120 requests — more than enough to drain the 100-token bucket.
        // We don't assert an exact count because greedy (time-based) refill
        // continuously trickles tokens back during the ~1s burst, so the real
        // cutoff is 100 + a few (typically ≤105 on fast hardware).
        int allowed = 0;
        for (int i = 0; i < 120; i++) {
            int status = mockMvc.perform(get("/api/v1/projects")
                            .header("Authorization", "Bearer " + tokenA))
                    .andReturn().getResponse().getStatus();
            if (status == 200) allowed++;
        }
        // At least 100 must have been allowed (the configured capacity).
        // At most 110 — a 10-token margin covers any refill during the burst.
        assertThat(allowed).isGreaterThanOrEqualTo(100).isLessThanOrEqualTo(110);

        // Bucket is now exhausted — next request for Tenant A is rate-limited
        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Rate limit exceeded"))
                .andExpect(header().exists("Retry-After"));

        // Tenant B's bucket is independent — still 200
        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());
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
