package com.danielchen.enterpriseapi;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Base class for integration tests.
 *
 * Two modes (set exactly one):
 *   DOCKER_API_COMPAT=true  — starts Testcontainers (used in CI / standard Docker)
 *   LOCAL_DOCKER=true       — connects to already-running local containers
 *                             (docker compose up postgres redis -d) on ports 5433 / 6379
 *
 * Tests are skipped automatically when neither variable is set.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    private static final boolean TC_MODE    = "true".equals(System.getenv("DOCKER_API_COMPAT"));
    private static final boolean LOCAL_MODE = "true".equals(System.getenv("LOCAL_DOCKER"));

    // Containers are only created in TC_MODE; null otherwise.
    private static PostgreSQLContainer<?> postgres;
    private static GenericContainer<?>    redis;

    static {
        if (TC_MODE) {
            postgres = new PostgreSQLContainer<>("postgres:16");
            redis    = new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
            postgres.start();
            redis.start();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        if (TC_MODE) {
            registry.add("spring.datasource.url",      postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.data.redis.url",
                    () -> "redis://localhost:" + redis.getMappedPort(6379));
        } else if (LOCAL_MODE) {
            registry.add("spring.datasource.url",      () -> "jdbc:postgresql://localhost:5433/enterpriseapi");
            registry.add("spring.datasource.username", () -> "app");
            registry.add("spring.datasource.password", () -> "secret");
            registry.add("spring.data.redis.url",      () -> "redis://localhost:6379");
        }
        // If neither mode is active the properties are left alone; @BeforeAll will skip the test.
    }

    @BeforeAll
    static void requireDockerEnvironment() {
        Assumptions.assumeTrue(
                TC_MODE || LOCAL_MODE,
                "Skipping integration test — set DOCKER_API_COMPAT=true (CI) or LOCAL_DOCKER=true (local dev)"
        );
    }
}
