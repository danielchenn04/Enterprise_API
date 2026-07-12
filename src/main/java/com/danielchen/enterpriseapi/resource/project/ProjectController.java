package com.danielchen.enterpriseapi.resource.project;

import com.danielchen.enterpriseapi.common.ApiResponse;
import com.danielchen.enterpriseapi.common.PageResponse;
import com.danielchen.enterpriseapi.resource.project.dto.CreateProjectRequest;
import com.danielchen.enterpriseapi.resource.project.dto.ProjectResponse;
import com.danielchen.enterpriseapi.resource.project.dto.UpdateProjectRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProjectResponse>> create(
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @GetMapping
    public ApiResponse<PageResponse<ProjectResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(projectService.list(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(projectService.get(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProjectResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request) {
        return ApiResponse.ok(projectService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
