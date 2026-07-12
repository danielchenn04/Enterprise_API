package com.danielchen.enterpriseapi.resource.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Page<Project> findByOrgId(UUID orgId, Pageable pageable);
    Optional<Project> findByIdAndOrgId(UUID id, UUID orgId);
    boolean existsByIdAndOrgId(UUID id, UUID orgId);
}
