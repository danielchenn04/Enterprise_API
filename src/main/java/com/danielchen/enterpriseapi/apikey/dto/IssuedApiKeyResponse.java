package com.danielchen.enterpriseapi.apikey.dto;

import com.danielchen.enterpriseapi.apikey.ApiKey;

/**
 * Returned only on key creation and rotation.
 * The rawKey is shown exactly once — it cannot be recovered after this response.
 */
public record IssuedApiKeyResponse(String rawKey, ApiKeyResponse key) {
    public static IssuedApiKeyResponse of(String rawKey, ApiKey key) {
        return new IssuedApiKeyResponse(rawKey, ApiKeyResponse.from(key));
    }
}
