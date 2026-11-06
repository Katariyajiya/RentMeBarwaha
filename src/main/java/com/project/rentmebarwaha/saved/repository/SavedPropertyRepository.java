package com.project.rentmebarwaha.saved.repository;

import com.project.rentmebarwaha.property.entity.PropertyStatus;
import com.project.rentmebarwaha.saved.entity.SavedProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface SavedPropertyRepository extends JpaRepository<SavedProperty, Long> {

    boolean existsByUserIdAndPropertyId(UUID userId, UUID propertyId);

    Optional<SavedProperty> findByUserIdAndPropertyId(UUID userId, UUID propertyId);

    void deleteByUserIdAndPropertyId(UUID userId, UUID propertyId);

    @Query("SELECT s FROM SavedProperty s JOIN FETCH s.property p WHERE s.user.id = :userId AND p.status = :status AND p.deletedAt IS NULL ORDER BY s.createdAt DESC")
    Page<SavedProperty> findByUserIdAndPropertyStatus(@Param("userId") UUID userId, @Param("status") PropertyStatus status, Pageable pageable);

    @Query("SELECT s.property.id FROM SavedProperty s WHERE s.user.id = :userId")
    Set<UUID> findPropertyIdsByUserId(@Param("userId") UUID userId);

    @Query("SELECT s.property.id FROM SavedProperty s WHERE s.user.id = :userId AND s.property.id IN :propertyIds")
    Set<UUID> findSavedPropertyIdsIn(@Param("userId") UUID userId, @Param("propertyIds") List<UUID> propertyIds);
}

