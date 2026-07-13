package com.danielchen.enterpriseapi.ratelimit;

import com.danielchen.enterpriseapi.security.AuthenticatedUser;
import com.danielchen.enterpriseapi.security.TenantContext;
import com.danielchen.enterpriseapi.tenant.OrgTier;
import com.danielchen.enterpriseapi.tenant.Organization;
import com.danielchen.enterpriseapi.tenant.OrganizationRepository;
import io.github.bucket4j.ConsumptionProbe;
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

    public RateLimitFilter(RateLimitService rateLimitService,
                           OrganizationRepository organizationRepository) {
        this.rateLimitService = rateLimitService;
        this.organizationRepository = organizationRepository;
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

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
        } else {
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded\"}");
        }
    }
}
