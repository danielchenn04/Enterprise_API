package com.danielchen.enterpriseapi.ratelimit;

import com.danielchen.enterpriseapi.security.AuthenticatedUser;
import com.danielchen.enterpriseapi.security.TenantContext;
import com.danielchen.enterpriseapi.tenant.OrgTier;
import com.danielchen.enterpriseapi.tenant.Organization;
import com.danielchen.enterpriseapi.tenant.OrganizationRepository;
import io.github.bucket4j.ConsumptionProbe;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final OrganizationRepository organizationRepository;
    private final MeterRegistry meterRegistry;

    public RateLimitFilter(RateLimitService rateLimitService,
                           OrganizationRepository organizationRepository,
                           MeterRegistry meterRegistry) {
        this.rateLimitService = rateLimitService;
        this.organizationRepository = organizationRepository;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        AuthenticatedUser user = TenantContext.get();
        if (user == null) {
            chain.doFilter(request, response);
            return;
        }

        OrgTier tier = organizationRepository.findById(user.orgId())
                .map(Organization::getTier)
                .orElse(OrgTier.FREE);

        ConsumptionProbe probe = rateLimitService.tryConsume(user.orgId(), tier);

        String orgTag = user.orgId().toString();
        if (probe.isConsumed()) {
            meterRegistry.counter("tenant.requests", "orgId", orgTag).increment();
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
        } else {
            meterRegistry.counter("rate.limit.exceeded", "orgId", orgTag).increment();
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded\"}");
        }
    }
}
