package com.project.rentmebarwaha.property.repository;

import com.project.rentmebarwaha.property.entity.PropertyView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface PropertyViewRepository extends JpaRepository<PropertyView, Long> {

    boolean existsByPropertyIdAndViewerIdentifierAndViewedAtAfter(UUID propertyId, String viewerIdentifier, Instant after);

    @Query("SELECT COUNT(v) FROM PropertyView v WHERE v.property.owner.id = :ownerId AND v.viewedAt BETWEEN :start AND :end")
    long countViewsByOwnerAndDateRange(@Param("ownerId") UUID ownerId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(v) FROM PropertyView v WHERE v.property.owner.id = :ownerId")
    long countTotalViewsByOwner(@Param("ownerId") UUID ownerId);
}

