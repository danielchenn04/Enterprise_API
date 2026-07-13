package com.danielchen.enterpriseapi.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private Map<String, TierConfig> tiers;

    @Data
    public static class TierConfig {
        private long capacity;
        private long refillTokens;
        private long refillPeriodSeconds;
    }
}
