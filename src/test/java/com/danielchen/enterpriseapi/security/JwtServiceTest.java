package com.danielchen.enterpriseapi.security;

import com.danielchen.enterpriseapi.tenant.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    // Base64 of a 256-bit secret — same default as application.yml
    private static final String SECRET =
            "c29tZS1zdXBlci1zZWNyZXQta2V5LWZvci1kZXYtb25seS1jaGFuZ2UtaW4tcHJvZA==";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86_400_000L); // 24h
    }

    @Test
    void issueAndParseRoundTrip() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        String token = jwtService.issue(userId, orgId, Role.ADMIN);
        Claims claims = jwtService.parse(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("orgId", String.class)).isEqualTo(orgId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void expiredTokenIsRejected() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1L); // already expired

        String token = jwtService.issue(UUID.randomUUID(), UUID.randomUUID(), Role.MEMBER);

        assertThatThrownBy(() -> jwtService.parse(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.issue(UUID.randomUUID(), UUID.randomUUID(), Role.ADMIN);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThatThrownBy(() -> jwtService.parse(tampered))
                .isInstanceOf(Exception.class);
    }
}
