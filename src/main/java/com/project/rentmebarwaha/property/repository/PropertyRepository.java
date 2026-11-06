package com.project.rentmebarwaha.property.repository;

import com.project.rentmebarwaha.property.entity.Property;
import com.project.rentmebarwaha.property.entity.PropertyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {

    Optional<Property> findByIdAndDeletedAtIsNull(UUID id);

    Page<Property> findByOwnerIdAndDeletedAtIsNull(UUID ownerId, Pageable pageable);

    Page<Property> findByOwnerIdAndStatusAndDeletedAtIsNull(UUID ownerId, PropertyStatus status, Pageable pageable);

    Page<Property> findByStatusAndDeletedAtIsNull(PropertyStatus status, Pageable pageable);

    long countByOwnerIdAndStatusAndDeletedAtIsNull(UUID ownerId, PropertyStatus status);

    long countByOwnerIdAndDeletedAtIsNull(UUID ownerId);

    @Query("SELECT DISTINCT p.locality FROM Property p WHERE LOWER(p.city) = LOWER(:city) AND p.status = 'ACTIVE' AND p.deletedAt IS NULL ORDER BY p.locality ASC")
    List<String> findDistinctLocalitiesByCity(@Param("city") String city);

    @Query("SELECT DISTINCT p.locality FROM Property p WHERE p.status = 'ACTIVE' AND p.deletedAt IS NULL ORDER BY p.locality ASC")
    List<String> findAllActiveLocalities();
}

