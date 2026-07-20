package com.danielchen.enterpriseapi;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Gated on DOCKER_API_COMPAT=true — Testcontainers 1.20.x hardcodes Docker API v1.32
// which OrbStack rejects (requires >=1.40). Set DOCKER_API_COMPAT=true in CI.
@EnabledIfEnvironmentVariable(named = "DOCKER_API_COMPAT", matches = "true")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
}
