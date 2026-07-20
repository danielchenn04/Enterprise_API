package com.danielchen.enterpriseapi.observability;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Exposes HikariCP connection pool statistics at /actuator/health.
 * Useful for spotting connection exhaustion before it causes 503s.
 * Shows as "connectionPool" in the health response.
 */
@Component("connectionPoolHealthIndicator")
public class ConnectionPoolHealthIndicator extends AbstractHealthIndicator {

    private final HikariDataSource hikariDataSource;

    public ConnectionPoolHealthIndicator(DataSource dataSource) {
        super("Connection pool health check failed");
        this.hikariDataSource = (HikariDataSource) dataSource;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
        if (pool == null) {
            builder.unknown().withDetail("info", "Pool not yet initialized");
            return;
        }
        int total = pool.getTotalConnections();
        int active = pool.getActiveConnections();
        int max = hikariDataSource.getMaximumPoolSize();

        Health.Builder status = (active < max) ? builder.up() : builder.outOfService();
        status.withDetail("activeConnections", active)
                .withDetail("idleConnections", pool.getIdleConnections())
                .withDetail("totalConnections", total)
                .withDetail("maxPoolSize", max)
                .withDetail("threadsAwaitingConnection", pool.getThreadsAwaitingConnection());
    }
}
