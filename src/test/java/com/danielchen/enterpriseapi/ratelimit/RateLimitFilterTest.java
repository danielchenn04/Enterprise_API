package com.danielchen.enterpriseapi.ratelimit;

import com.danielchen.enterpriseapi.security.AuthenticatedUser;
import com.danielchen.enterpriseapi.security.TenantContext;
import com.danielchen.enterpriseapi.tenant.OrgTier;
import com.danielchen.enterpriseapi.tenant.Organization;
import com.danielchen.enterpriseapi.tenant.OrganizationRepository;
import com.danielchen.enterpriseapi.tenant.Role;
import io.github.bucket4j.ConsumptionProbe;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    RateLimitService rateLimitService;

    @Mock
    OrganizationRepository organizationRepository;

    @Mock
    FilterChain chain;

    private RateLimitFilter filter;
    private SimpleMeterRegistry meterRegistry;

    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        filter = new RateLimitFilter(rateLimitService, organizationRepository, meterRegistry);
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void unauthenticatedRequestPassesThrough() throws Exception {
        // No TenantContext set → filter must not touch the request
        var req = new MockHttpServletRequest();
        var res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        verifyNoInteractions(rateLimitService);
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void allowedRequestForwardsAndSetsHeader() throws Exception {
        TenantContext.set(new AuthenticatedUser(null, orgId, Role.ADMIN));

        Organization org = new Organization();
        org.setTier(OrgTier.FREE);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(42L);
        when(rateLimitService.tryConsume(eq(orgId), eq(OrgTier.FREE))).thenReturn(probe);

        var req = new MockHttpServletRequest();
        var res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        assertThat(res.getHeader("X-RateLimit-Remaining")).isEqualTo("42");
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void rateLimitedRequestReturns429() throws Exception {
        TenantContext.set(new AuthenticatedUser(null, orgId, Role.ADMIN));

        Organization org = new Organization();
        org.setTier(OrgTier.FREE);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(false);
        when(probe.getNanosToWaitForRefill()).thenReturn(30_000_000_000L); // 30s
        when(rateLimitService.tryConsume(eq(orgId), eq(OrgTier.FREE))).thenReturn(probe);

        var req = new MockHttpServletRequest();
        var res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("Retry-After")).isEqualTo("30");
        assertThat(res.getContentAsString()).contains("Rate limit exceeded");
    }

    @Test
    void rateLimitExceededCounterIncrements() throws Exception {
        TenantContext.set(new AuthenticatedUser(null, orgId, Role.ADMIN));

        Organization org = new Organization();
        org.setTier(OrgTier.FREE);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(false);
        when(probe.getNanosToWaitForRefill()).thenReturn(0L);
        when(rateLimitService.tryConsume(any(), any())).thenReturn(probe);

        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        double count = meterRegistry.counter("rate.limit.exceeded",
                "orgId", orgId.toString()).count();
        assertThat(count).isEqualTo(1.0);
    }
}
