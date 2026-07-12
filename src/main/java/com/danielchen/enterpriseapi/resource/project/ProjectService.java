package com.danielchen.enterpriseapi.resource.project;

import com.danielchen.enterpriseapi.common.NotFoundException;
import com.danielchen.enterpriseapi.resource.project.dto.CreateProjectRequest;
import com.danielchen.enterpriseapi.resource.project.dto.ProjectResponse;
import com.danielchen.enterpriseapi.resource.project.dto.UpdateProjectRequest;
import com.danielchen.enterpriseapi.security.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        UUID orgId = currentOrgId();
        applyRls(orgId);

        Project project = new Project();
        project.setOrgId(orgId);
        project.setName(request.name());
        project.setDescription(request.description());
        return ProjectResponse.from(projectRepository.save(project));
    }

    public Page<ProjectResponse> list(Pageable pageable) {
        UUID orgId = currentOrgId();
        applyRls(orgId);
        return projectRepository.findByOrgId(orgId, pageable).map(ProjectResponse::from);
    }

    public ProjectResponse get(UUID id) {
        UUID orgId = currentOrgId();
        applyRls(orgId);
        return projectRepository.findByIdAndOrgId(id, orgId)
                .map(ProjectResponse::from)
                .orElseThrow(() -> new NotFoundException("Project not found: " + id));
    }

    @Transactional
    public ProjectResponse update(UUID id, UpdateProjectRequest request) {
        UUID orgId = currentOrgId();
        applyRls(orgId);

        Project project = projectRepository.findByIdAndOrgId(id, orgId)
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
        UUID orgId = currentOrgId();
        applyRls(orgId);

        if (!projectRepository.existsByIdAndOrgId(id, orgId)) {
            throw new NotFoundException("Project not found: " + id);
        }
        projectRepository.deleteById(id);
    }

    /**
     * Sets app.current_org for the current transaction so PostgreSQL RLS policies
     * can filter rows by tenant. SET LOCAL scopes the variable to this transaction only.
     */
    private void applyRls(UUID orgId) {
        em.createNativeQuery("SET LOCAL app.current_org = '" + orgId + "'")
                .executeUpdate();
    }

    private UUID currentOrgId() {
        return TenantContext.get().orgId();
    }
}
