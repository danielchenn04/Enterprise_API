package com.danielchen.enterpriseapi.apikey;

import com.danielchen.enterpriseapi.apikey.dto.CreateApiKeyRequest;
import com.danielchen.enterpriseapi.common.NotFoundException;
import com.danielchen.enterpriseapi.security.AuthenticatedUser;
import com.danielchen.enterpriseapi.security.TenantContext;
import com.danielchen.enterpriseapi.tenant.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    ApiKeyRepository repository;

    @InjectMocks
    ApiKeyService service;

    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(new AuthenticatedUser(UUID.randomUUID(), orgId, Role.ADMIN));
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void generateKeyHasCorrectPrefixAndLength() {
        String key = ApiKeyService.generateKey();
        assertThat(key).startsWith("eak_");
        // eak_ (4) + base64url of 32 bytes without padding = 43 chars → total 47
        assertThat(key).hasSize(47);
    }

    @Test
    void hashKeyProducesSha256HexOf64Chars() {
        String hash = ApiKeyService.hashKey("test-key");
        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void hashKeyIsDeterministic() {
        assertThat(ApiKeyService.hashKey("same")).isEqualTo(ApiKeyService.hashKey("same"));
        assertThat(ApiKeyService.hashKey("a")).isNotEqualTo(ApiKeyService.hashKey("b"));
    }

    @Test
    void issueReturnsRawKeyAndSavesHash() {
        when(repository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.issue(new CreateApiKeyRequest("ci-key", Role.MEMBER));

        assertThat(response.rawKey()).startsWith("eak_");
        assertThat(response.key().name()).isEqualTo("ci-key");
        assertThat(response.key().role()).isEqualTo(Role.MEMBER);
        assertThat(response.key().keyHint()).endsWith("...");

        verify(repository).save(argThat(k ->
                k.getKeyHash().equals(ApiKeyService.hashKey(response.rawKey()))
                        && k.getOrgId().equals(orgId)));
    }

    @Test
    void revokeThrowsWhenKeyNotFound() {
        when(repository.findByIdAndOrgId(any(), eq(orgId))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revoke(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rotateRevokesOldAndReturnsNewKey() {
        ApiKey old = new ApiKey();
        old.setOrgId(orgId);
        old.setName("my-key");
        old.setRole(Role.ADMIN);
        old.setKeyHash("oldhash");
        old.setKeyHint("eak_XXXXXX...");

        when(repository.findByIdAndOrgId(any(), eq(orgId))).thenReturn(Optional.of(old));
        when(repository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.rotate(old.getId() != null ? old.getId() : UUID.randomUUID());

        assertThat(old.getRevokedAt()).isNotNull();
        assertThat(response.rawKey()).startsWith("eak_");
        assertThat(response.key().name()).isEqualTo("my-key");
        assertThat(response.key().role()).isEqualTo(Role.ADMIN);
    }
}
