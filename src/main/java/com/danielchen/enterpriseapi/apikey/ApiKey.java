package com.danielchen.enterpriseapi.apikey;

import com.danielchen.enterpriseapi.tenant.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false, updatable = false)
    private UUID orgId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, updatable = false)
    private String keyHash;

    @Column(nullable = false, updatable = false)
    private String keyHint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private Instant revokedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public boolean isActive() {
        return revokedAt == null;
    }
}
