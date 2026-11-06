package com.project.rentmebarwaha.visit.repository;

import com.project.rentmebarwaha.visit.entity.Visit;
import com.project.rentmebarwaha.visit.entity.VisitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface VisitRepository extends JpaRepository<Visit, UUID> {

    Page<Visit> findByOwnerIdOrderByScheduledAtAsc(UUID ownerId, Pageable pageable);

    Page<Visit> findByTenantIdOrderByScheduledAtAsc(UUID tenantId, Pageable pageable);

    @Query("SELECT v FROM Visit v WHERE v.owner.id = :ownerId AND v.scheduledAt >= :after ORDER BY v.scheduledAt ASC")
    List<Visit> findUpcomingVisitsByOwner(@Param("ownerId") UUID ownerId, @Param("after") Instant after, Pageable pageable);

    long countByPropertyIdAndStatus(UUID propertyId, VisitStatus status);

    long countByOwnerIdAndStatus(UUID ownerId, VisitStatus status);

    long countByPropertyId(UUID propertyId);
}

