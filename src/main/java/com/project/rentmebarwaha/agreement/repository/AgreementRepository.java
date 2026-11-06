package com.project.rentmebarwaha.agreement.repository;

import com.project.rentmebarwaha.agreement.entity.Agreement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, UUID> {

    Page<Agreement> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId, Pageable pageable);

    Page<Agreement> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}

