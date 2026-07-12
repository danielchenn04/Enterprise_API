package com.danielchen.enterpriseapi.security;

import com.danielchen.enterpriseapi.apikey.ApiKey;
import com.danielchen.enterpriseapi.apikey.ApiKeyRepository;
import com.danielchen.enterpriseapi.apikey.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyAuthFilter(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("ApiKey ")) {
            chain.doFilter(request, response);
            return;
        }

        String rawKey = header.substring(7).strip();
        String keyHash = ApiKeyService.hashKey(rawKey);

        ApiKey apiKey = apiKeyRepository.findByKeyHashAndRevokedAtIsNull(keyHash).orElse(null);
        if (apiKey == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"success\":false,\"message\":\"Invalid or revoked API key\"}");
            return;
        }

        // userId is null for machine clients — only orgId and role matter
        AuthenticatedUser authUser = new AuthenticatedUser(null, apiKey.getOrgId(), apiKey.getRole());
        TenantContext.set(authUser);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                authUser, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + apiKey.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
