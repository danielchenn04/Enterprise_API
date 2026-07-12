package com.danielchen.enterpriseapi.security;

import com.danielchen.enterpriseapi.tenant.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
import java.util.UUID;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtService.parse(token);
            UUID userId = UUID.fromString(claims.getSubject());
            UUID orgId  = UUID.fromString(claims.get("orgId", String.class));
            Role role   = Role.valueOf(claims.get("role", String.class));

            AuthenticatedUser authUser = new AuthenticatedUser(userId, orgId, role);
            TenantContext.set(authUser);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    authUser, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (JwtException | IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"success\":false,\"message\":\"Invalid or expired token\"}");
            return;
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
