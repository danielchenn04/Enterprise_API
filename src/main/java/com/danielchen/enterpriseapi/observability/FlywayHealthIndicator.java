package com.danielchen.enterpriseapi.observability;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

/**
 * Exposes the current Flyway migration version at /actuator/health.
 * Complements the built-in "db" indicator (connection check) with schema version detail.
 * Shows as "flyway" in the health response.
 */
@Component("flywayHealthIndicator")
public class FlywayHealthIndicator extends AbstractHealthIndicator {

    private final Flyway flyway;

    public FlywayHealthIndicator(Flyway flyway) {
        super("Flyway health check failed");
        this.flyway = flyway;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        MigrationInfo current = flyway.info().current();
        if (current == null) {
            builder.unknown().withDetail("info", "No migrations applied yet");
            return;
        }
        builder.up()
                .withDetail("version", current.getVersion().getVersion())
                .withDetail("description", current.getDescription())
                .withDetail("state", current.getState().getDisplayName());
    }
}
