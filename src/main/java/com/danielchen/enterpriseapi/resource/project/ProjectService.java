package com.danielchen.enterpriseapi.resource.project;

import com.danielchen.enterpriseapi.common.NotFoundException;
import com.danielchen.enterpriseapi.resource.project.dto.CreateProjectRequest;
import com.danielchen.enterpriseapi.resource.project.dto.ProjectResponse;
import com.danielchen.enterpriseapi.resource.project.dto.UpdateProjectRequest;
import lombok.RequiredArgsConstructor;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        return ProjectResponse.from(projectRepository.save(project));
    }

    public Page<ProjectResponse> list(Pageable pageable) {
        return projectRepository.findAll(pageable).map(ProjectResponse::from);
    }

    public ProjectResponse get(UUID id) {
        return projectRepository.findById(id)
                .map(ProjectResponse::from)
                .orElseThrow(() -> new NotFoundException("Project not found: " + id));
    }

    @Transactional
    public ProjectResponse update(UUID id, UpdateProjectRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found: " + id));
        if (request.name() != null) {
            project.setName(request.name());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        project.setUpdatedAt(Instant.now());
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(UUID id) {
        if (!projectRepository.existsById(id)) {
            throw new NotFoundException("Project not found: " + id);
        }
        projectRepository.deleteById(id);
    }
}
