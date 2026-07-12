package com.danielchen.enterpriseapi.apikey;

import com.danielchen.enterpriseapi.apikey.dto.ApiKeyResponse;
import com.danielchen.enterpriseapi.apikey.dto.CreateApiKeyRequest;
import com.danielchen.enterpriseapi.apikey.dto.IssuedApiKeyResponse;
import com.danielchen.enterpriseapi.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/apikeys")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    public ResponseEntity<ApiResponse<IssuedApiKeyResponse>> issue(
            @Valid @RequestBody CreateApiKeyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(apiKeyService.issue(request)));
    }

    @GetMapping
    public ApiResponse<List<ApiKeyResponse>> list() {
        return ApiResponse.ok(apiKeyService.list());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        apiKeyService.revoke(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rotate")
    public ApiResponse<IssuedApiKeyResponse> rotate(@PathVariable UUID id) {
        return ApiResponse.ok(apiKeyService.rotate(id));
    }
}
