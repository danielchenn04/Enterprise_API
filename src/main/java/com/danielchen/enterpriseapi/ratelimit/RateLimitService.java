package com.danielchen.enterpriseapi.ratelimit;

import com.danielchen.enterpriseapi.tenant.OrgTier;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties properties;

    public ConsumptionProbe tryConsume(UUID orgId, OrgTier tier) {
        BucketConfiguration config = buildConfig(tier);
        return proxyManager.builder()
                .build("rl:org:" + orgId, () -> config)
                .tryConsumeAndReturnRemaining(1);
    }

    private BucketConfiguration buildConfig(OrgTier tier) {
        RateLimitProperties.TierConfig tc =
                properties.getTiers().get(tier.name().toLowerCase());
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(tc.getCapacity())
                        .refillGreedy(tc.getRefillTokens(),
                                Duration.ofSeconds(tc.getRefillPeriodSeconds()))
                        .build())
                .build();
    }
}
