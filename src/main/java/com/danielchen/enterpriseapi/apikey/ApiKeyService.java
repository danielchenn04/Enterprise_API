package com.danielchen.enterpriseapi.apikey;

import com.danielchen.enterpriseapi.apikey.dto.ApiKeyResponse;
import com.danielchen.enterpriseapi.apikey.dto.CreateApiKeyRequest;
import com.danielchen.enterpriseapi.apikey.dto.IssuedApiKeyResponse;
import com.danielchen.enterpriseapi.common.NotFoundException;
import com.danielchen.enterpriseapi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public IssuedApiKeyResponse issue(CreateApiKeyRequest request) {
        UUID orgId = TenantContext.get().orgId();
        String rawKey = generateKey();

        ApiKey apiKey = new ApiKey();
        apiKey.setOrgId(orgId);
        apiKey.setName(request.name());
        apiKey.setKeyHash(hashKey(rawKey));
        apiKey.setKeyHint(rawKey.substring(0, 12) + "...");
        apiKey.setRole(request.role());
        apiKeyRepository.save(apiKey);

        return IssuedApiKeyResponse.of(rawKey, apiKey);
    }

    public List<ApiKeyResponse> list() {
        UUID orgId = TenantContext.get().orgId();
        return apiKeyRepository.findByOrgId(orgId).stream()
                .map(ApiKeyResponse::from)
                .toList();
    }

    @Transactional
    public void revoke(UUID id) {
        UUID orgId = TenantContext.get().orgId();
        ApiKey key = apiKeyRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new NotFoundException("API key not found: " + id));
        key.setRevokedAt(Instant.now());
    }

    @Transactional
    public IssuedApiKeyResponse rotate(UUID id) {
        UUID orgId = TenantContext.get().orgId();
        ApiKey old = apiKeyRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new NotFoundException("API key not found: " + id));

        // Revoke old key
        old.setRevokedAt(Instant.now());

        // Issue replacement with same name and role
        String rawKey = generateKey();
        ApiKey replacement = new ApiKey();
        replacement.setOrgId(orgId);
        replacement.setName(old.getName());
        replacement.setKeyHash(hashKey(rawKey));
        replacement.setKeyHint(rawKey.substring(0, 12) + "...");
        replacement.setRole(old.getRole());
        apiKeyRepository.save(replacement);

        return IssuedApiKeyResponse.of(rawKey, replacement);
    }

    // --- Static utilities also used by ApiKeyAuthFilter ---

    public static String generateKey() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return "eak_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashKey(String rawKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
