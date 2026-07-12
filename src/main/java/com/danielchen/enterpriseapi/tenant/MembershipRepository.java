package com.danielchen.enterpriseapi.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    List<Membership> findByUser_Id(UUID userId);
    Optional<Membership> findByOrganization_IdAndUser_Id(UUID orgId, UUID userId);
}
