package com.danielchen.enterpriseapi.config;

import com.danielchen.enterpriseapi.apikey.ApiKeyRepository;
import com.danielchen.enterpriseapi.ratelimit.RateLimitFilter;
import com.danielchen.enterpriseapi.ratelimit.RateLimitService;
import com.danielchen.enterpriseapi.security.ApiKeyAuthFilter;
import com.danielchen.enterpriseapi.security.JwtAuthFilter;
import com.danielchen.enterpriseapi.security.JwtService;
import com.danielchen.enterpriseapi.tenant.OrganizationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final ApiKeyRepository apiKeyRepository;
    private final RateLimitService rateLimitService;
    private final OrganizationRepository organizationRepository;
    private final MeterRegistry meterRegistry;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new ApiKeyAuthFilter(apiKeyRepository), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new RateLimitFilter(rateLimitService, organizationRepository, meterRegistry),
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"success\":false,\"message\":\"Authentication required\"}");
                        })
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
